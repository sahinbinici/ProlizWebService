package com.prolizwebservices.service;

import com.prolizwebservices.entity.ServiceDependency;
import com.prolizwebservices.repository.ServiceDependencyRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SOAP servisleri arasındaki bağımlılıkları yöneten servis
 * 
 * Bağımlılık Zinciri Örneği:
 * UzaktanEgitimDersleri (DERS_HAR_ID) 
 *   -> UzaktanEgitimDersiAlanOgrencileri (dersHarID)
 *   -> DersiVerenOgretimElamaniGetir (OGRETIM_ELEMANI_TC)
 * 
 * Bu servis:
 * 1. Bağımlılıkları tanımlar ve saklar
 * 2. Bağımlılık zincirlerini çözer
 * 3. Doğru sırada veri çekme işlemlerini organize eder
 * 4. Cache invalidation'da cascade işlemleri yönetir
 */
@Service
public class ServiceDependencyManager {

    private static final Logger logger = LoggerFactory.getLogger(ServiceDependencyManager.class);

    @Autowired
    private ServiceDependencyRepository dependencyRepository;

    // In-memory bağımlılık haritası (performans için)
    private Map<String, List<ServiceDependency>> dependencyMap = new HashMap<>();

    /**
     * Uygulama başlarken bağımlılıkları initialize et
     */
    @PostConstruct
    @Transactional
    public void initializeDependencies() {
        logger.info("🔗 Servis bağımlılıkları initialize ediliyor...");

        // Mevcut bağımlılıkları kontrol et
        long existingCount = dependencyRepository.count();
        
        if (existingCount == 0) {
            // İlk kez çalışıyor, default bağımlılıkları oluştur
            createDefaultDependencies();
        }

        // Bağımlılıkları memory'ye yükle
        loadDependenciesToMemory();

        logger.info("✅ {} servis bağımlılığı yüklendi", dependencyMap.size());
    }

    /**
     * Default bağımlılıkları oluştur
     */
    @Transactional
    private void createDefaultDependencies() {
        logger.info("📝 Default servis bağımlılıkları oluşturuluyor...");

        List<ServiceDependency> dependencies = new ArrayList<>();

        // 1. UzaktanEgitimDersleri -> UzaktanEgitimDersiAlanOgrencileri
        ServiceDependency dep1 = new ServiceDependency();
        dep1.setParentService("UzaktanEgitimDersleri");
        dep1.setChildService("UzaktanEgitimDersiAlanOgrencileri");
        dep1.setParentFieldName("DERS_HAR_ID");
        dep1.setChildParameterName("dersHarID");
        dep1.setDescription("Ders listesinden ders ID'leri alınır, her ders için öğrenci listesi çekilir");
        dep1.setPriority(1);
        dep1.setCreatedAt(LocalDateTime.now());
        dep1.setActive(true);
        dependencies.add(dep1);

        // 2. UzaktanEgitimDersleri -> DersiVerenOgretimElamaniGetir
        ServiceDependency dep2 = new ServiceDependency();
        dep2.setParentService("UzaktanEgitimDersleri");
        dep2.setChildService("DersiVerenOgretimElamaniGetir");
        dep2.setParentFieldName("OGRETIM_ELEMANI_TC");
        dep2.setChildParameterName("tc_kimlik_no");
        dep2.setDescription("Ders listesinden öğretim elemanı TC'leri alınır, detayları çekilir");
        dep2.setPriority(2);
        dep2.setCreatedAt(LocalDateTime.now());
        dep2.setActive(true);
        dependencies.add(dep2);

        // 3. UzaktanEgitimDersiAlanOgrencileri -> OgrenciBilgileri (gelecek için)
        ServiceDependency dep3 = new ServiceDependency();
        dep3.setParentService("UzaktanEgitimDersiAlanOgrencileri");
        dep3.setChildService("OgrenciBilgileri");
        dep3.setParentFieldName("OGR_NO");
        dep3.setChildParameterName("ogrenciNo");
        dep3.setDescription("Öğrenci listesinden öğrenci numaraları alınır, detayları çekilir");
        dep3.setPriority(3);
        dep3.setCreatedAt(LocalDateTime.now());
        dep3.setActive(false); // Şimdilik pasif
        dependencies.add(dep3);

        dependencyRepository.saveAll(dependencies);
        logger.info("✅ {} default bağımlılık oluşturuldu", dependencies.size());
    }

    /**
     * Bağımlılıkları memory'ye yükle
     */
    private void loadDependenciesToMemory() {
        dependencyMap.clear();
        
        List<ServiceDependency> allDependencies = dependencyRepository.findByActiveTrueOrderByPriorityAsc();
        
        for (ServiceDependency dep : allDependencies) {
            dependencyMap.computeIfAbsent(dep.getParentService(), k -> new ArrayList<>()).add(dep);
        }
    }

    /**
     * Belirli bir servisin bağımlı servislerini getir
     */
    public List<ServiceDependency> getDependencies(String parentService) {
        return dependencyMap.getOrDefault(parentService, Collections.emptyList());
    }

    /**
     * Bağımlılık zincirini çöz (topological sort)
     * 
     * @param startService Başlangıç servisi
     * @return Çalıştırılması gereken servislerin sıralı listesi
     */
    public List<String> resolveDependencyChain(String startService) {
        List<String> executionOrder = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        
        resolveDependencyChainRecursive(startService, executionOrder, visited);
        
        logger.debug("🔗 Bağımlılık zinciri çözüldü: {} -> {}", startService, executionOrder);
        return executionOrder;
    }

    /**
     * Recursive bağımlılık çözümleyici
     */
    private void resolveDependencyChainRecursive(String service, List<String> executionOrder, Set<String> visited) {
        if (visited.contains(service)) {
            return; // Döngüsel bağımlılık kontrolü
        }
        
        visited.add(service);
        
        // Önce bu servisin bağımlılıklarını çöz
        List<ServiceDependency> dependencies = getDependencies(service);
        for (ServiceDependency dep : dependencies) {
            resolveDependencyChainRecursive(dep.getChildService(), executionOrder, visited);
        }
        
        // Sonra bu servisi ekle
        executionOrder.add(service);
    }

    /**
     * Bir servis invalidate edildiğinde, bağımlı servisleri de invalidate et
     * 
     * @param service Invalidate edilecek servis
     * @return Invalidate edilmesi gereken tüm servislerin listesi
     */
    public List<String> getCascadeInvalidationList(String service) {
        Set<String> toInvalidate = new HashSet<>();
        toInvalidate.add(service);
        
        // Bu servise bağımlı olan tüm servisleri bul
        findDependentServices(service, toInvalidate);
        
        logger.info("🗑️ Cascade invalidation: {} -> {}", service, toInvalidate);
        return new ArrayList<>(toInvalidate);
    }

    /**
     * Recursive olarak bağımlı servisleri bul
     */
    private void findDependentServices(String service, Set<String> result) {
        // Bu servise bağımlı olan servisleri bul
        List<ServiceDependency> dependents = dependencyRepository.findByChildServiceAndActiveTrue(service);
        
        for (ServiceDependency dep : dependents) {
            if (!result.contains(dep.getParentService())) {
                result.add(dep.getParentService());
                findDependentServices(dep.getParentService(), result);
            }
        }
    }

    /**
     * Yeni bağımlılık ekle
     */
    @Transactional
    public ServiceDependency addDependency(String parentService, String childService, 
                                          String parentField, String childParameter, 
                                          String description, Integer priority) {
        ServiceDependency dependency = new ServiceDependency();
        dependency.setParentService(parentService);
        dependency.setChildService(childService);
        dependency.setParentFieldName(parentField);
        dependency.setChildParameterName(childParameter);
        dependency.setDescription(description);
        dependency.setPriority(priority != null ? priority : 1);
        dependency.setCreatedAt(LocalDateTime.now());
        dependency.setActive(true);
        
        ServiceDependency saved = dependencyRepository.save(dependency);
        
        // Memory'yi güncelle
        loadDependenciesToMemory();
        
        logger.info("✅ Yeni bağımlılık eklendi: {} -> {}", parentService, childService);
        return saved;
    }

    /**
     * Bağımlılığı devre dışı bırak
     */
    @Transactional
    public void disableDependency(Long dependencyId) {
        dependencyRepository.findById(dependencyId).ifPresent(dep -> {
            dep.setActive(false);
            dependencyRepository.save(dep);
            loadDependenciesToMemory();
            logger.info("🔴 Bağımlılık devre dışı bırakıldı: {} -> {}", 
                dep.getParentService(), dep.getChildService());
        });
    }

    /**
     * Tüm bağımlılıkları getir
     */
    public List<ServiceDependency> getAllDependencies() {
        return dependencyRepository.findByActiveTrueOrderByPriorityAsc();
    }

    /**
     * Bağımlılık grafiğini görselleştir (Mermaid format)
     */
    public String generateDependencyGraph() {
        StringBuilder mermaid = new StringBuilder();
        mermaid.append("graph TD\n");
        
        List<ServiceDependency> allDeps = getAllDependencies();
        for (ServiceDependency dep : allDeps) {
            mermaid.append(String.format("    %s[%s] -->|%s| %s[%s]\n",
                dep.getParentService().replaceAll("[^a-zA-Z0-9]", ""),
                dep.getParentService(),
                dep.getParentFieldName(),
                dep.getChildService().replaceAll("[^a-zA-Z0-9]", ""),
                dep.getChildService()
            ));
        }
        
        return mermaid.toString();
    }

    /**
     * Döngüsel bağımlılık kontrolü
     */
    public boolean hasCyclicDependency(String service) {
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        
        return hasCyclicDependencyRecursive(service, visited, recursionStack);
    }

    private boolean hasCyclicDependencyRecursive(String service, Set<String> visited, Set<String> recursionStack) {
        if (recursionStack.contains(service)) {
            logger.warn("⚠️ Döngüsel bağımlılık tespit edildi: {}", service);
            return true;
        }
        
        if (visited.contains(service)) {
            return false;
        }
        
        visited.add(service);
        recursionStack.add(service);
        
        List<ServiceDependency> dependencies = getDependencies(service);
        for (ServiceDependency dep : dependencies) {
            if (hasCyclicDependencyRecursive(dep.getChildService(), visited, recursionStack)) {
                return true;
            }
        }
        
        recursionStack.remove(service);
        return false;
    }

    /**
     * Bağımlılık istatistikleri
     */
    public Map<String, Object> getDependencyStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        List<ServiceDependency> allDeps = getAllDependencies();
        stats.put("totalDependencies", allDeps.size());
        
        // Servis başına bağımlılık sayısı
        Map<String, Long> parentCounts = allDeps.stream()
            .collect(Collectors.groupingBy(ServiceDependency::getParentService, Collectors.counting()));
        stats.put("dependenciesByParent", parentCounts);
        
        // En çok bağımlılığı olan servis
        String mostDependencies = parentCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("N/A");
        stats.put("mostDependentService", mostDependencies);
        
        // Maksimum bağımlılık derinliği
        int maxDepth = allDeps.stream()
            .mapToInt(dep -> resolveDependencyChain(dep.getParentService()).size())
            .max()
            .orElse(0);
        stats.put("maxDependencyDepth", maxDepth);
        
        return stats;
    }
}
