package com.ks.bayyinah.bayyinah_server.repository;

import com.ks.bayyinah.bayyinah_server.model.UserStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserStatisticsRepository extends JpaRepository<UserStatistics, Long> {

  Optional<UserStatistics> findByUserId(Long userId);

  void deleteByUserId(Long userId);
}
