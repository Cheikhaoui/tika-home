package com.tikahome.repository;

import com.tikahome.entity.Channel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChannelRepository extends JpaRepository<Channel, UUID> {

    List<Channel> findByEnabledTrue();

    List<Channel> findByPropertyIdAndEnabledTrue(UUID propertyId);
}
