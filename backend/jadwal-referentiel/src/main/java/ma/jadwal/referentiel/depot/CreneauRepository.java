package ma.jadwal.referentiel.depot;

import ma.jadwal.referentiel.entite.Creneau;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CreneauRepository extends JpaRepository<Creneau, Long> {

    List<Creneau> findByEtablissementIdOrderByJourAscIndexDebutAsc(Long etablissementId);

    void deleteByEtablissementId(Long etablissementId);
}
