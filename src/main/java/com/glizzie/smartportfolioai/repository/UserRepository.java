package com.glizzie.smartportfolioai.repository;

import com.glizzie.smartportfolioai.config.BotUsers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<BotUsers, Long> {
}
