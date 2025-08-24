package streetwalker.userservice.models.community;


import jakarta.persistence.*;

@Entity
@Table(name = "community_roles")
public class CommunityRole {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name; // OWNER, MODERATOR, MEMBER
    private String permissions;
}
