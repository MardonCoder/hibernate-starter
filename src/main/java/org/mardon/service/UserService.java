package org.mardon.service;

import jakarta.persistence.AttributeNode;
import jakarta.persistence.EntityGraph;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.hibernate.graph.GraphSemantic;
import org.mardon.dao.UserRepository;
import org.mardon.dto.UserCreateDto;
import org.mardon.dto.UserReadDto;
import org.mardon.entity.User;
import org.mardon.mapper.Mapper;
import org.mardon.mapper.UserCreateMapper;
import org.mardon.mapper.UserReadMapper;

import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserReadMapper userReadMapper;
    private final UserCreateMapper userCreateMapper;

    @Transactional
    public Long create(UserCreateDto userDto){

        var userEntity = userCreateMapper.mapFrom(userDto);
        return userRepository.save(userEntity).getId();
    }

    @Transactional
    public <T> Optional<T> findById(Long id, Mapper<User, T > mapper){
        var graph = userRepository.getEntityManager().createEntityGraph(User.class);
        graph.addAttributeNode("company");
        Map<String, Object> properties = Map.of(
                GraphSemantic.LOAD.getJakartaHintName(), graph
        );
        return userRepository.findById(id, properties)
                .map(mapper::mapFrom);
    }

    @Transactional
    public Optional<UserReadDto> findById(Long id){
        return findById(id, userReadMapper);
    }

    @Transactional
    public boolean delete(User entity){
        var maybeUser = userRepository.findById(entity.getId());
        maybeUser.ifPresent(userRepository::delete);
        return maybeUser.isPresent();
    }
}
