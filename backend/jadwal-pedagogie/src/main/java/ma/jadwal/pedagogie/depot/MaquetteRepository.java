package ma.jadwal.pedagogie.depot;

import ma.jadwal.pedagogie.entite.Maquette;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MaquetteRepository extends JpaRepository<Maquette, Long> {

    List<Maquette> findByEtablissementId(Long etablissementId);

    List<Maquette> findByNiveauIdAndEtablissementId(Long niveauId, Long etablissementId);

    Optional<Maquette> findByIdAndEtablissementId(Long id, Long etablissementId);

    void deleteByNiveauId(Long niveauId);
}
