package com.playconnect.repository;

import com.playconnect.entity.Court;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourtRepo extends JpaRepository<Court, Long>{
    
}
