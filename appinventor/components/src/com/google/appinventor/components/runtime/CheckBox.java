// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2018 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.components.runtime;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.common.YaVersion;

/**
 * ![Example of a CheckBox](images/checkbox.png)
 *
 * `CheckBox` components can detect user taps and can change their boolean state in response.
 *
 * A `CheckBox` component raises an event when the user taps it. There are many properties affecting
 * its appearance that can be set in the Designer or Blocks Editor.
 */
@DesignerComponent(version = YaVersion.CHECKBOX_COMPONENT_VERSION,
    description = "Checkbox that raises an event when the user clicks on it. " +
    "There are many properties affecting its appearance that can be set in " +
    "the Designer or Blocks Editor.",
    category = ComponentCategory.USERINTERFACE)
@SimpleObject
public final class CheckBox extends ToggleBase<android.widget.CheckBox> {

  /**
   * Creates a new CheckBox component.
   *
   * @param container  container, component will be placed in
   */
  public CheckBox(ComponentContainer container) {
    super(container);
    view = new android.widget.CheckBox(container.$context());
    Checked(false);
    initToggle();
  }

  /**
   * Set to `true`{:.logic.block} if the box is checked, `false`{:.logic.block} otherwise.
   *
   * @return  {@code true} indicates checked, {@code false} unchecked
   */
  @SimpleProperty(
      category = PropertyCategory.BEHAVIOR,
      description = "True if the box is checked, false otherwise.")
  public boolean Checked() {
    return view.isChecked();
  }

  /**
   * Checked property setter method.
   *
   * @suppressdoc
   * @param value  {@code true} indicates checked, {@code false} unchecked
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,
      defaultValue = "False")
  @SimpleProperty
  public void Checked(boolean value) {
    view.setChecked(value);
    view.invalidate();
  }

  // START LinkedData

  private String propertyUri;

  /**
   *
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_PROPERTY_URI,
          defaultValue = "")
  @SimpleProperty(category = PropertyCategory.LINKED_DATA,
          description = "<p>Property URI specifies the relationship between a "
                  + "Linked Data Form containing a TextBox, Password, etc. and the "
                  + "component. Common properties include the name properties in the "
                  + "Friend-of-a-Friend ontology (e.g. foaf:name, foaf:givenName, "
                  + "foaf:surname), label properties (e.g. rdfs:label, skos:prefLabel), "
                  + "or descriptions (e.g. rdfs:comment, dc:description).</p>")
  public void PropertyURI(String uri) {
    this.propertyUri = uri;
  }

  /**
   *
   */
  @SimpleProperty
  public String PropertyURI() {
    return propertyUri;
  }

  public String ObjectType() {
    return "xsd:boolean";
  }

  public void ObjectType(String uri) {}

  public boolean SubjectIdentifier() {
    return false;
  }

  public Object Value() {
    return Checked();
  }

  // END LinkedData
}
