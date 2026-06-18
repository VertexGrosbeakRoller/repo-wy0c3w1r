package tech.javelin.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import ru.nexusguard.protection.annotations.Native;
import tech.javelin.base.events.impl.player.EventUpdate;
import tech.javelin.client.modules.api.Category;
import tech.javelin.client.modules.api.Module;
import tech.javelin.client.modules.api.ModuleAnnotation;
import tech.javelin.utility.game.other.MessageUtil;
import tech.javelin.utility.interfaces.IClient;
import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ModuleAnnotation(
   name = "NeuroRotation",
   description = "Обучаемая ротация на основе записанных движений (.cfg style)",
   category = Category.COMBAT
)
public final class NeuroRotation extends Module implements IClient {
   public static final NeuroRotation INSTANCE = new NeuroRotation();
   
   private boolean recording = false;
   private boolean playing = false;
   private String currentDataset = "default";
   private List<RotationData> recordedRotations = new ArrayList<>();
   private int currentIndex = 0;
   private File neuroDir;
   private Gson gson = new GsonBuilder().setPrettyPrinting().create();
   
   // Store all loaded datasets
   private Map<String, List<RotationData>> datasets = new HashMap<>();

   private NeuroRotation() {
      neuroDir = new File(MinecraftClient.getInstance().runDirectory, "Javelin/neuro");
      if (!neuroDir.exists()) neuroDir.mkdirs();
   }

   @EventTarget
   @Native
   public void onUpdate(EventUpdate e) {
      if (mc.player == null) return;
      
      if (recording) {
         recordedRotations.add(new RotationData(
            mc.player.getYaw(),
            mc.player.getPitch(),
            System.currentTimeMillis()
         ));
      }
      
      if (playing && !recordedRotations.isEmpty() && currentIndex < recordedRotations.size()) {
         RotationData data = recordedRotations.get(currentIndex);
         mc.player.setYaw(data.yaw);
         mc.player.setPitch(data.pitch);
         currentIndex++;
         if (currentIndex >= recordedRotations.size()) {
            currentIndex = 0;
         }
      }
   }
   
   public void startRecording(String name) {
      if (name == null || name.isEmpty()) name = "default";
      currentDataset = sanitizeName(name);
      recording = true;
      recordedRotations = new ArrayList<>();
      MessageUtil.displayInfo("§a[Neuro] §fЗапись начата: §e" + currentDataset);
   }
   
   public void stopRecording() {
      if (!recording) {
         MessageUtil.displayInfo("§c[Neuro] §fЗапись не активна");
         return;
      }
      recording = false;
      datasets.put(currentDataset, new ArrayList<>(recordedRotations));
      MessageUtil.displayInfo("§a[Neuro] §fЗапись остановлена. Точек: §e" + recordedRotations.size());
   }
   
   public void saveDataset(String name) {
      if (name == null || name.isEmpty()) name = currentDataset;
      name = sanitizeName(name);
      
      List<RotationData> data = datasets.get(name);
      if (data == null) data = recordedRotations;
      if (data == null || data.isEmpty()) {
         MessageUtil.displayInfo("§c[Neuro] §fНет данных для сохранения");
         return;
      }
      
      File file = new File(neuroDir, name + ".json");
      try (FileWriter writer = new FileWriter(file)) {
         gson.toJson(data, writer);
         MessageUtil.displayInfo("§a[Neuro] §fСохранено: §e" + name + " §f(" + data.size() + " точек)");
      } catch (IOException ex) {
         MessageUtil.displayInfo("§c[Neuro] §fОшибка: §e" + ex.getMessage());
      }
   }
   
   public void loadDataset(String name) {
      if (name == null || name.isEmpty()) name = "default";
      name = sanitizeName(name);
      
      File file = new File(neuroDir, name + ".json");
      if (!file.exists()) {
         MessageUtil.displayInfo("§c[Neuro] §fФайл не найден: §e" + name);
         return;
      }
      
      try (FileReader reader = new FileReader(file)) {
         List<RotationData> data = gson.fromJson(reader, new TypeToken<List<RotationData>>(){}.getType());
         if (data == null) data = new ArrayList<>();
         datasets.put(name, data);
         recordedRotations = new ArrayList<>(data);
         currentDataset = name;
         MessageUtil.displayInfo("§a[Neuro] §fЗагружено: §e" + name + " §f(" + data.size() + " точек)");
      } catch (IOException ex) {
         MessageUtil.displayInfo("§c[Neuro] §fОшибка загрузки: §e" + ex.getMessage());
      }
   }
   
   public void listDatasets() {
      File[] files = neuroDir.listFiles((dir, name) -> name.endsWith(".json"));
      if (files == null || files.length == 0) {
         MessageUtil.displayInfo("§e[Neuro] §fНет сохраненных датасетов");
         return;
      }
      
      StringBuilder sb = new StringBuilder("§a[Neuro] §fДатасеты:\n");
      for (File f : files) {
         String name = f.getName().replace(".json", "");
         String active = name.equals(currentDataset) ? " §7[ACTIVE]" : "";
         sb.append("§7- §f").append(name).append(active).append("\n");
      }
      MessageUtil.displayInfo(sb.toString());
   }
   
   public void deleteDataset(String name) {
      if (name == null || name.isEmpty()) {
         MessageUtil.displayInfo("§c[Neuro] §fУкажите имя: .neuro del <name>");
         return;
      }
      name = sanitizeName(name);
      
      File file = new File(neuroDir, name + ".json");
      if (!file.exists()) {
         MessageUtil.displayInfo("§c[Neuro] §fФайл не найден: §e" + name);
         return;
      }
      
      if (file.delete()) {
         datasets.remove(name);
         if (currentDataset.equals(name)) {
            currentDataset = "default";
            recordedRotations.clear();
         }
         MessageUtil.displayInfo("§a[Neuro] §fУдалено: §e" + name);
      } else {
         MessageUtil.displayInfo("§c[Neuro] §fНе удалось удалить");
      }
   }
   
   public void play(String name) {
      if (name != null && !name.isEmpty()) {
         name = sanitizeName(name);
         if (!name.equals(currentDataset) || !datasets.containsKey(name)) {
            loadDataset(name);
         }
      }
      
      List<RotationData> data = datasets.get(currentDataset);
      if (data == null) data = recordedRotations;
      if (data == null || data.isEmpty()) {
         MessageUtil.displayInfo("§c[Neuro] §fНет данных для воспроизведения");
         return;
      }
      
      recordedRotations = new ArrayList<>(data);
      playing = true;
      currentIndex = 0;
      MessageUtil.displayInfo("§a[Neuro] §fВоспроизведение: §e" + currentDataset);
   }
   
   public void stop() {
      playing = false;
      recording = false;
      MessageUtil.displayInfo("§a[Neuro] §fОстановлено");
   }
   
   public String getCurrentDataset() {
      return currentDataset;
   }
   
   public boolean isRecording() {
      return recording;
   }
   
   public boolean isPlaying() {
      return playing;
   }
   
   private String sanitizeName(String name) {
      return name.replaceAll("[^a-zA-Z0-9_-]", "").toLowerCase();
   }
   
   @Override
   public void onDisable() {
      super.onDisable();
      recording = false;
      playing = false;
   }
   
   private static class RotationData {
      public float yaw;
      public float pitch;
      public long timestamp;
      
      public RotationData(float yaw, float pitch, long timestamp) {
         this.yaw = yaw;
         this.pitch = pitch;
         this.timestamp = timestamp;
      }
   }
}
