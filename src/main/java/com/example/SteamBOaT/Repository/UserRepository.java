package alafonin4.SteamBOaT.Repository;

import alafonin4.SteamBOaT.Entity.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends CrudRepository<User, Long> {

}
