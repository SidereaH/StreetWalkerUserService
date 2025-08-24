package streetwalker.userservice.models.community;


import jakarta.persistence.*;
import streetwalker.userservice.models.User;

import java.time.OffsetDateTime;

@Entity
@Table(name = "community_memberships")
public class CommunityMembership {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    @ManyToOne
    private CommunityProfile community;

    @ManyToOne
    private CommunityRole role;

    private OffsetDateTime joinedAt;
}
