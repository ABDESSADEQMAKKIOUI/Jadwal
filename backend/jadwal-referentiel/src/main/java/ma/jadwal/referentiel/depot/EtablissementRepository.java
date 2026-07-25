package ma.jadwal.referentiel.depot;

import ma.jadwal.referentiel.entite.Etablissement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EtablissementRepository extends JpaRepository<Etablissement, Long> {

    boolean existsByCode(String code);
}
