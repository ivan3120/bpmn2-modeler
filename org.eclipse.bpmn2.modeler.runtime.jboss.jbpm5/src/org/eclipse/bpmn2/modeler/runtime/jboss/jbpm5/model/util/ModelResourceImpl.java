/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 *  All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 *
 * @author Bob Brodt
 ******************************************************************************/
/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.model.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.bpmn2.modeler.core.model.Bpmn2ModelerResourceImpl;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.impl.EAttributeImpl;
import org.eclipse.emf.ecore.util.BasicFeatureMap;
import org.eclipse.emf.ecore.util.ExtendedMetaData;
import org.eclipse.emf.ecore.xmi.XMLHelper;
import org.eclipse.emf.ecore.xmi.XMLLoad;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMLLoadImpl;
import org.xml.sax.helpers.DefaultHandler;

/**
 * <!-- begin-user-doc -->
 * The <b>Resource </b> associated with the package.
 * <!-- end-user-doc -->
 * @see org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.model.util.ModelResourceFactoryImpl
 * @generated NOT
 */
public class ModelResourceImpl extends Bpmn2ModelerResourceImpl {
	
	/**
	 * Creates an instance of the resource.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param uri the URI of the new resource.
	 * @generated NOT
	 */
	public ModelResourceImpl(URI uri) {
		super(uri);
	}


    /**
     * Override this method to hook in our own XmlHandler
     */
    @Override
    protected XMLLoad createXMLLoad() {
        return new XMLLoadImpl(createXMLHelper()) {
            @Override
            protected DefaultHandler makeDefaultHandler() {
                return new ModelerXmlHandler(resource, helper, options);
            }
        };
    }

    /**
     * We need extend the standard SAXXMLHandler to hook into the handling of attribute references
     * which may be either simple ID Strings or QNames. We'll search through all of the objects'
     * IDs first to find the one we're looking for. If not, we'll try a QName search.
     */
    protected static class ModelerXmlHandler extends BpmnXmlHandler {

        public ModelerXmlHandler(XMLResource xmiResource, XMLHelper helper, Map<?, ?> options) {
            super(xmiResource, helper, options);
        }

		@SuppressWarnings("unchecked")
		@Override
		protected void processElement(String name, String prefix, String localName) {
			
			super.processElement(name, prefix, localName);
			
			// ugly hack for https://bugs.eclipse.org/bugs/show_bug.cgi?id=355686
			// Remove the "type" attribute from the feature map if there is one.
			// The XSI type will have already been used to construct the EObject,
			// so any "type" in the feature map will be a duplicate which will
			// cause problems during parsing.
			// See also getXSIType()
			try {
				EObject childObject = objects.peekEObject();
				if (childObject!=null) {
					EStructuralFeature anyAttribute = childObject.eClass().getEStructuralFeature("anyAttribute");
					if (anyAttribute!=null) {
						List<BasicFeatureMap.Entry> anyMap = (List<BasicFeatureMap.Entry>)childObject.eGet(anyAttribute);
						List<BasicFeatureMap.Entry> removed = new ArrayList<BasicFeatureMap.Entry>();
						for (BasicFeatureMap.Entry fe : anyMap) {
							if (fe.getEStructuralFeature() instanceof EAttribute) {
								EAttributeImpl a = (EAttributeImpl)fe.getEStructuralFeature();
								String n = a.getName();
								String ns = a.getExtendedMetaData().getNamespace();
								if (TYPE.equals(n) && XSI_URI.equals(ns)) {
									removed.add(fe);
								}
							}
						}
						if (removed.size()>0)
							anyMap.removeAll(removed);
					}
				}
			}
			catch(Exception e) {
			}
		}

		@Override
		protected String getXSIType() {
			if (isNamespaceAware)
				return attribs.getValue(ExtendedMetaData.XSI_URI,
						XMLResource.TYPE);

			// If an element specifies multiple xsi:type data types, the last one wins.
			// NOTE: we must check for "type" in any namespace with the URI
			// "http://www.w3.org/2001/XMLSchema-instance"
			String value = null;
			int length = attribs.getLength();
			for (int i = 0; i < length; ++i) {
				attribs.getQName(i);
				String localpart= attribs.getLocalName(i);
				String prefix = null;
				int ci = localpart.lastIndexOf(':');
				if (ci>0) {
					prefix = localpart.substring(0, ci); 
					localpart = localpart.substring(ci+1);
				}
				if (TYPE.equals(localpart)) {
					String uri = helper.getNamespaceURI(prefix);
					if (XSI_URI.equals(uri)) {
						value = attribs.getValue(i);
					}
				}
			}
			return value;
		}

		/**
         * Overridden to be able to convert ID references in attributes to URIs during load.
         * If the reference can't be found by its ID, we'll try a QName search (done in the
         * super class)
         * @param ids
         *  In our case the parameter will contain exactly one ID that we resolve to URI.
         */
        @Override
        protected void setValueFromId(EObject object, EReference eReference, String ids) {

            for (EObject o : objects) {
                TreeIterator<EObject> iter = o.eAllContents();
                while (iter.hasNext()) {
                    EObject obj = iter.next();
                    EStructuralFeature feature = ((EObject) obj).eClass().getEIDAttribute();
                    if (feature != null && obj.eGet(feature) != null) {
                        Object id = obj.eGet(feature);
                        if (id!=null && id.equals(ids)) {
                        	try {
                        		object.eSet(eReference,obj);
                        	}
                        	catch(Exception e) {
                        		continue;
                        	}
                        	return;
                        }
                    }
                }
            }

            super.setValueFromId(object,eReference,ids);
        }
    }
} //ModelResourceImpl
