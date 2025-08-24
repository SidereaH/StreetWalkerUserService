package streetwalker.userservice.models.community;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import streetwalker.userservice.models.Profile;
import streetwalker.userservice.models.ProfileType;
import streetwalker.userservice.models.User;

@Entity
@Table(name = "community_profiles")
@PrimaryKeyJoinColumn(name = "profile_id")
public class CommunityProfile extends Profile {


    @ManyToOne
    private User owner;
    @Override
    public ProfileType getType() {
        return ProfileType.COMMUNITY;
    }
}
