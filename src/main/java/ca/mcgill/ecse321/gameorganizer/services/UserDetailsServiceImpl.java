package ca.mcgill.ecse321.gameorganizer.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import ca.mcgill.ecse321.gameorganizer.models.Account;
import ca.mcgill.ecse321.gameorganizer.models.GameOwner;
import ca.mcgill.ecse321.gameorganizer.repositories.AccountRepository;

@Primary

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private AccountRepository accountRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Optional<Account> accountOpt = accountRepository.findByEmail(email);
        if (accountOpt.isEmpty()) {
            throw new UsernameNotFoundException("User not found with email: " + email);
        }

        Account account = accountOpt.get();
        List<GrantedAuthority> authorities = new ArrayList<>();

        // Assign authorities based on account type, using ROLE_ prefix convention
        authorities.add(new SimpleGrantedAuthority("ROLE_USER")); 

        if (account instanceof GameOwner) {
            authorities.add(new SimpleGrantedAuthority("ROLE_GAME_OWNER"));
        }
        // Add other roles/authorities if needed based on other account subtypes, e.g., ROLE_ADMIN
        System.out.println(account.getEmail());
        return new User(account.getEmail(), account.getPassword(), authorities);
    }
}
