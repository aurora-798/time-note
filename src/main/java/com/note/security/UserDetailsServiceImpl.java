package com.note.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.note.entity.SysUser;
import com.note.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserService sysUserService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser sysUser = sysUserService.getOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (sysUser == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        return new User(
                sysUser.getUsername(),
                sysUser.getPassword(),
                sysUser.getStatus() == 1,
                true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_" + sysUser.getRole().toUpperCase())));
    }
}
