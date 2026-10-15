package com.spendy.auth.Repository;

import com.spendy.auth.Data.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface IUserRepository extends JpaRepository<User, String> {

    User findByEmail(String email);

    User findByUsername(String username);

    @Query("SELECT u FROM User u WHERE u.id_user = :id_user")
    User findByIdUser(@Param("id_user") String id_user);

    User findByHouseUser(String house_user);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE User u SET u.houseUser = :houseCode WHERE u.username = :username")
    void setHouseUser(@Param("username") String username, @Param("houseCode") String houseCode);

    @Query("SELECT u FROM User u WHERE u.houseUser = :houseId")
    List<User> findByHouseId(@Param("houseId") String houseId);
}