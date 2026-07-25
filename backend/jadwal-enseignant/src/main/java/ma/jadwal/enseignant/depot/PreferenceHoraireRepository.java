package ma.jadwal.enseignant.depot;

import ma.jadwal.enseignant.entite.PreferenceHoraire;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PreferenceHoraireRepository extends JpaRepository<PreferenceHoraire, Long> {

    List<PreferenceHoraire> findByEnseignantIdOrderByJourAscIndexDebutAsc(Long enseignantId);

    void deleteByEnseignantId(Long enseignantId);
}
