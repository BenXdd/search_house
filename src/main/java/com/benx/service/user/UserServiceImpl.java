package com.benx.service.user;

import com.benx.entity.Role;
import com.benx.entity.User;
import com.benx.repository.RoleRepository;
import com.benx.repository.UserRepository;
import com.benx.service.IUserService;
import com.benx.service.ServiceResult;
import com.benx.web.dto.UserDTO;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserServiceImpl implements IUserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public User findUserByName(String userName) {
        User user =  userRepository.findByName(userName);

        if (user == null){
            return null;
        }
        List<Role> roleList = roleRepository.findRolesByUserId(user.getId());
        if (roleList == null || roleList.isEmpty()){
            throw new DisabledException("权限非法");
        }

        ArrayList<GrantedAuthority> authorities = new ArrayList<>();
        roleList.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_"+role.getName())));
        user.setAuthorityList(authorities);
        return user;
    }

    @Override
    public ServiceResult<UserDTO> findById(Long userId) {
        User user = userRepository.findOne(userId);
        if (user == null) {
            return ServiceResult.notFound();
        }
        UserDTO userDTO = modelMapper.map(user, UserDTO.class);
        return ServiceResult.of(userDTO);
    }
}
