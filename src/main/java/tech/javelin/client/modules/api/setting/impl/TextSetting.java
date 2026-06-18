package tech.javelin.client.modules.api.setting.impl;

import com.google.gson.JsonObject;
import java.util.function.Supplier;
import lombok.Generated;
import tech.javelin.client.modules.api.setting.Setting;

public class TextSetting extends Setting {
   private String value;
   private final String defaultValue;

   public TextSetting(String name, String defaultValue) {
      super(name);
      this.value = defaultValue;
      this.defaultValue = defaultValue;
   }

   public TextSetting(String name, String defaultValue, Supplier<Boolean> visible) {
      super(name);
      this.value = defaultValue;
      this.defaultValue = defaultValue;
      this.setVisible(visible);
   }

   public void safe(JsonObject propertiesObject) {
      propertiesObject.addProperty(this.name, this.value);
   }

   public void load(JsonObject propertiesObject) {
      this.value = propertiesObject.get(this.name).getAsString();
   }

   public String get() {
      return this.value;
   }

   public void set(String value) {
      this.value = value;
   }

   @Generated
   public String getDefaultValue() {
      return this.defaultValue;
   }
}
