package com.example.mr_backend_wh.converter;


import com.example.mr_backend_wh.DTO.UsuarioDTO;
import com.example.mr_backend_wh.model.Usuario;
import org.mapstruct.Mapper;

import java.util.List;


@Mapper(componentModel = "spring")
public interface UsuarioMapper {
  UsuarioDTO toDTO(Usuario entity);
  Usuario toEntity(UsuarioDTO dto);
  List<UsuarioDTO> toDTO(List<Usuario> listEntity);
  List<Usuario> toEntity(List<UsuarioDTO> listDTOs);
}
