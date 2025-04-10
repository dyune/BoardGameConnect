package ca.mcgill.ecse321.gameorganizer.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ca.mcgill.ecse321.gameorganizer.models.Account;
import ca.mcgill.ecse321.gameorganizer.models.GameOwner;
import ca.mcgill.ecse321.gameorganizer.repositories.AccountRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AccountRepository accountRepository;

    public CustomUserDetailsService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        
        // Create a list to hold the authorities
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        
        // All authenticated users get ROLE_USER
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        
        // If the account is a GameOwner, also add ROLE_GAME_OWNER
        if (account instanceof GameOwner) {
            authorities.add(new SimpleGrantedAuthority("ROLE_GAME_OWNER"));
        }
        
        return new User(account.getEmail(), account.getPassword(), authorities);
    }
}
