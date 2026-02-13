package com.cda.carpooling.service;

import com.cda.carpooling.dto.request.BrandRequest;
import com.cda.carpooling.dto.response.BrandResponse;
import com.cda.carpooling.entity.Brand;
import com.cda.carpooling.exception.DuplicateResourceException;
import com.cda.carpooling.exception.ResourceNotFoundException;
import com.cda.carpooling.mapper.BrandMapper;
import com.cda.carpooling.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BrandService {
    private final BrandMapper brandMapper;

    private final BrandRepository brandRepository;

    /**
     * Récupère toutes les marques triées par nom.
     */
    @Transactional(readOnly = true)
    public List<BrandResponse> getAllBrands() {
        return brandRepository.findAllByOrderByNameAsc()
                .stream()
                .map(brandMapper::toResponse)
                .toList();
    }

    /**
     * Récupère une marque par son ID.
     */
    @Transactional(readOnly = true)
    public BrandResponse getBrandById(Long id) {
        Brand brand = findBrandOrThrow(id);
        return brandMapper.toResponse(brand);
    }

    /**
     * Crée une nouvelle marque.
     */
    @Transactional
    public BrandResponse createBrand(BrandRequest request) {
        if (brandRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException(
                    "Une marque avec le nom '" + request.getName() + "' existe déjà"
            );
        }

        Brand brand = Brand.builder()
                .name(request.getName())
                .build();

        Brand saved = brandRepository.save(brand);

        return brandMapper.toResponse(saved);
    }

    /**
     * Met à jour une marque existante.
     */
    @Transactional
    public BrandResponse updateBrand(Long id, BrandRequest request) {
        Brand brand = findBrandOrThrow(id);

        if (!brand.getName().equals(request.getName())
                && brandRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException(
                    "Une marque avec le nom '" + request.getName() + "' existe déjà"
            );
        }

        brand.setName(request.getName());
        Brand updated = brandRepository.save(brand);
        return brandMapper.toResponse(updated);
    }

    /**
     * Supprime une marque.
     * Échoue si des véhicules sont associés à cette marque.
     */
    @Transactional
    public void deleteBrand(Long id) {
        Brand brand = findBrandOrThrow(id);

        if (!brand.getVehicles().isEmpty()) {
            throw new IllegalStateException(
                    "Impossible de supprimer la marque '" + brand.getName()
                            + "' : " + brand.getVehicles().size() + " véhicule(s) y sont associés"
            );
        }

        brandRepository.delete(brand);
    }

    //region Utils
    private Brand findBrandOrThrow(Long id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Marque", "id", id));
    }
    //endregion
}