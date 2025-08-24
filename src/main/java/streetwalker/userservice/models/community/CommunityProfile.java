package streetwalker.userservice.models.community;

import jakarta.persistence.*;
import streetwalker.userservice.models.Profile;
import streetwalker.userservice.models.ProfileType;
import streetwalker.userservice.models.User;

import java.util.List;

@Entity
@Table(name = "community_profiles")
@PrimaryKeyJoinColumn(name = "profile_id")
public class CommunityProfile extends Profile {

    @OneToMany(mappedBy = "community_profile", cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<CommunityMembership> membership;



    @ManyToOne
    private User owner;
    @Override
    public ProfileType getType() {
        return ProfileType.COMMUNITY;
    }
}
