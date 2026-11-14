package br.com.hubdeservicos.repository.usuario;

import br.com.hubdeservicos.entity.usuario.MunicipioEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MunicipioRepository extends JpaRepository<MunicipioEntity, Integer> {
    @Query("""
            select m from MunicipioEntity m
             where m.nomeBusca = :nome and (m.uf = :sigla or m.ufBusca = :estado)
            """)
    Optional<MunicipioEntity> buscar(String nome, String sigla, String estado);
}
