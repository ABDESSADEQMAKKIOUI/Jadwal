package ma.jadwal.referentiel.depot;

import ma.jadwal.referentiel.entite.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    Optional<Utilisateur> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Utilisateur> findByEtablissement_IdOrderById(Long etablissementId);
}
