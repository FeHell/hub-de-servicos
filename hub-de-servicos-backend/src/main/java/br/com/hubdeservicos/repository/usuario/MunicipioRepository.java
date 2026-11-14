package br.com.hubdeservicos.repository.usuario;

import br.com.hubdeservicos.entity.usuario.MunicipioEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MunicipioRepository extends JpaRepository<MunicipioEntity, Integer> {
    @Query("""
            select m from MunicipioEntity m
             where m.nomeBusca = :nome and (m.uf = :sigla or m.ufBusca = :estado)
            """)
    Optional<MunicipioEntity> buscar(String nome, String sigla, String estado);

    @Query("""
            select m from MunicipioEntity m where m.nomeBusca like :termo escape '!'
             order by case when m.nomeBusca like :prefixo escape '!' then 0 else 1 end, m.nomeBusca, m.uf
            """)
    List<MunicipioEntity> pesquisar(String termo, String prefixo, Pageable pagina);
}
