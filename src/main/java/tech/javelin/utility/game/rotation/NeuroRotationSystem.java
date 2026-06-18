package tech.javelin.utility.game.rotation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.MinecraftClient;
import tech.javelin.utility.game.other.MessageUtil;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NeuroRotationSystem {
   private static final NeuroRotationSystem INSTANCE = new NeuroRotationSystem();
   private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
   private final File datasetsDir;
   
   private boolean recording = false;
   private String currentRecordingName = "";
   private List<RotationSample> currentRecording = new ArrayList<>();
   private Map<String, NeuroDataset> loadedDatasets = new HashMap<>();
   
   public static NeuroRotationSystem getInstance() {
      return INSTANCE;
   }
   
   private NeuroRotationSystem() {
      File configDir = new File(MinecraftClient.getInstance().runDirectory, "Javelin");
      if (!configDir.exists()) configDir.mkdirs();
      this.datasetsDir = new File(configDir, "neuro_datasets");
      if (!datasetsDir.exists()) datasetsDir.mkdirs();
   }
   
   public void startRecording(String name) {
      if (recording) {
         MessageUtil.displayInfo("§c[Neuro] §fУже идет запись: §e" + currentRecordingName);
         return;
      }
      recording = true;
      currentRecordingName = name;
      currentRecording.clear();
      MessageUtil.displayInfo("§a[Neuro] §fНачата запись датасета: §e" + name);
   }
   
   public void stopRecording() {
      if (!recording) {
         MessageUtil.displayInfo("§c[Neuro] §fЗапись не активна");
         return;
      }
      recording = false;
      
      if (currentRecording.isEmpty()) {
         MessageUtil.displayInfo("§c[Neuro] §fНет данных для сохранения");
         return;
      }
      
      // Сохраняем датасет
      NeuroDataset dataset = new NeuroDataset(currentRecordingName, currentRecording);
      saveDataset(dataset);
      loadedDatasets.put(currentRecordingName, dataset);
      
      MessageUtil.displayInfo("§a[Neuro] §fЗапись §e" + currentRecordingName + " §fсохранена. Точек: §e" + currentRecording.size());
      currentRecording.clear();
   }
   
   public void recordSample(float yaw, float pitch, double x, double y, double z, boolean onGround, boolean swinging) {
      if (!recording) return;
      
      currentRecording.add(new RotationSample(
         yaw, pitch, x, y, z, onGround, swinging,
         System.currentTimeMillis()
      ));
   }
   
   public NeuroDataset loadDataset(String name) {
      if (loadedDatasets.containsKey(name)) {
         return loadedDatasets.get(name);
      }
      
      File file = new File(datasetsDir, name + ".json");
      if (!file.exists()) {
         MessageUtil.displayInfo("§c[Neuro] §fДатасет не найден: §e" + name);
         return null;
      }
      
      try (FileReader reader = new FileReader(file)) {
         List<RotationSample> samples = gson.fromJson(reader, new TypeToken<List<RotationSample>>(){}.getType());
         if (samples == null) samples = new ArrayList<>();
         
         NeuroDataset dataset = new NeuroDataset(name, samples);
         loadedDatasets.put(name, dataset);
         
         // Строим статистику
         buildRotationStats(dataset);
         
         MessageUtil.displayInfo("§a[Neuro] §fЗагружен датасет: §e" + name + " §f(§e" + samples.size() + " §fточек)");
         return dataset;
      } catch (IOException e) {
         MessageUtil.displayInfo("§c[Neuro] §fОшибка загрузки: §e" + e.getMessage());
         return null;
      }
   }
   
   public void saveDataset(NeuroDataset dataset) {
      File file = new File(datasetsDir, dataset.getName() + ".json");
      try (FileWriter writer = new FileWriter(file)) {
         gson.toJson(dataset.getSamples(), writer);
      } catch (IOException e) {
         MessageUtil.displayInfo("§c[Neuro] §fОшибка сохранения: §e" + e.getMessage());
      }
   }
   
   public void buildRotationStats(NeuroDataset dataset) {
      List<RotationSample> samples = dataset.getSamples();
      if (samples.size() < 2) return;
      
      // Анализируем паттерны ротации
      float avgYawDelta = 0, avgPitchDelta = 0;
      float maxYawDelta = 0, maxPitchDelta = 0;
      int attackCount = 0;
      
      for (int i = 1; i < samples.size(); i++) {
         RotationSample prev = samples.get(i - 1);
         RotationSample curr = samples.get(i);
         
         float yawDelta = Math.abs(wrapDegrees(curr.yaw - prev.yaw));
         float pitchDelta = Math.abs(curr.pitch - prev.pitch);
         
         avgYawDelta += yawDelta;
         avgPitchDelta += pitchDelta;
         maxYawDelta = Math.max(maxYawDelta, yawDelta);
         maxPitchDelta = Math.max(maxPitchDelta, pitchDelta);
         
         if (curr.swinging && !prev.swinging) {
            attackCount++;
         }
      }
      
      avgYawDelta /= (samples.size() - 1);
      avgPitchDelta /= (samples.size() - 1);
      
      // Генерируем паттерны для воспроизведения
      List<GeneratedRotation> patterns = new ArrayList<>();
      
      // Создаем разнообразные паттерны на основе статистики
      for (int i = 0; i < 5; i++) {
         patterns.add(new GeneratedRotation(
            avgYawDelta * (0.8f + i * 0.1f),
            avgPitchDelta * (0.8f + i * 0.1f),
            100 + i * 50  // Длительность в мс
         ));
      }
      
      dataset.setPatterns(patterns);
      
      MessageUtil.displayInfo("§a[Neuro] §fАнализ: Ср. ΔY=§e" + String.format("%.2f", avgYawDelta) + 
         "§f, Ср. ΔP=§e" + String.format("%.2f", avgPitchDelta) +
         "§f, Атак=§e" + attackCount);
   }
   
   public GeneratedRotation generateRotation(NeuroDataset dataset, float targetYaw, float targetPitch, float currentYaw, float currentPitch) {
      if (dataset == null || dataset.getPatterns().isEmpty()) {
         return null;
      }
      
      List<GeneratedRotation> patterns = dataset.getPatterns();
      
      // Выбираем паттерн с добавлением случайности
      int index = (int) (Math.random() * patterns.size());
      GeneratedRotation base = patterns.get(index);
      
      // Добавляем вариации
      float yawDelta = wrapDegrees(targetYaw - currentYaw);
      float pitchDelta = targetPitch - currentPitch;
      
      float factor = Math.min(1.0f, (float) Math.sqrt(yawDelta * yawDelta + pitchDelta * pitchDelta) / 45.0f);
      
      float speed = base.speed * (0.9f + (float) Math.random() * 0.2f);
      float yawStep = yawDelta * speed * factor;
      float pitchStep = pitchDelta * speed * factor;
      
      // Добавляем "человеческий" шум
      yawStep += (float) (Math.random() - 0.5) * base.yawVariation * 0.3f;
      pitchStep += (float) (Math.random() - 0.5) * base.pitchVariation * 0.3f;
      
      return new GeneratedRotation(yawStep, pitchStep, base.duration);
   }
   
   public boolean isRecording() {
      return recording;
   }
   
   public String getCurrentRecordingName() {
      return currentRecordingName;
   }
   
   public List<String> getAvailableDatasets() {
      List<String> names = new ArrayList<>();
      File[] files = datasetsDir.listFiles((dir, name) -> name.endsWith(".json"));
      if (files != null) {
         for (File file : files) {
            names.add(file.getName().replace(".json", ""));
         }
      }
      return names;
   }
   
   public NeuroDataset getLastLoadedDataset() {
      if (loadedDatasets.isEmpty()) return null;
      // Return the first loaded dataset (most recent)
      return loadedDatasets.values().iterator().next();
   }
   
   public NeuroDataset getDataset(String name) {
      return loadedDatasets.get(name);
   }
   
   private float wrapDegrees(float degrees) {
      degrees %= 360.0f;
      if (degrees >= 180.0f) degrees -= 360.0f;
      if (degrees < -180.0f) degrees += 360.0f;
      return degrees;
   }
   
   // Классы данных
   public static class RotationSample {
      public float yaw, pitch;
      public double x, y, z;
      public boolean onGround, swinging;
      public long timestamp;
      
      public RotationSample(float yaw, float pitch, double x, double y, double z, 
                           boolean onGround, boolean swinging, long timestamp) {
         this.yaw = yaw;
         this.pitch = pitch;
         this.x = x;
         this.y = y;
         this.z = z;
         this.onGround = onGround;
         this.swinging = swinging;
         this.timestamp = timestamp;
      }
   }
   
   public static class NeuroDataset {
      private final String name;
      private final List<RotationSample> samples;
      private List<GeneratedRotation> patterns = new ArrayList<>();
      
      public NeuroDataset(String name, List<RotationSample> samples) {
         this.name = name;
         this.samples = samples;
      }
      
      public String getName() { return name; }
      public List<RotationSample> getSamples() { return samples; }
      public List<GeneratedRotation> getPatterns() { return patterns; }
      public void setPatterns(List<GeneratedRotation> patterns) { this.patterns = patterns; }
   }
   
   public static class GeneratedRotation {
      public float yawVariation;
      public float pitchVariation;
      public float speed;
      public int duration;
      
      public GeneratedRotation(float yawVariation, float pitchVariation, int duration) {
         this.yawVariation = yawVariation;
         this.pitchVariation = pitchVariation;
         this.speed = 0.15f + (float) Math.random() * 0.1f;
         this.duration = duration;
      }
   }
}
