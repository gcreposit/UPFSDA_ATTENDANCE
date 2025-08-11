package com.example.demo.security;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

public class CustomUserDetailService implements UserDetailsService {

    @Autowired
    private UserRepository userRepo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Optional<User> userData = userRepo.findByUsername(username) ;

        User user = User.builder().id(userData.get().getId())
                .username(userData.get().getUsername())
                .password(userData.get().getPassword())
                .role(userData.get().getRole()).build();

        return new CustomUserDetail(user);
    }

}
