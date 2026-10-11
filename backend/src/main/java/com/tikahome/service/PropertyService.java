package com.tikahome.service;

import com.tikahome.entity.Property;
import com.tikahome.exception.NotFoundException;
import com.tikahome.repository.PropertyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PropertyService {

    private final PropertyRepository properties;

    public PropertyService(PropertyRepository properties) {
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public Property get(UUID id) {
        return properties.findById(id).orElseThrow(() -> new NotFoundException("Property " + id + " not found"));
    }
}
