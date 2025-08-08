package echonest.sociogram.connectus.Models;

public class ModelUser {
    // use same name as in firebase
    String coverPhoto, profilePhoto,  email, password,search,userId,name,onlineStatus, profession, publicKey;
    int followerCount;
    public ModelUser(){}

    public ModelUser(String coverPhoto, String profilePhoto, String email, String password, String search, String userId, String name, String onlineStatus, String profession, int followerCount, String publicKey) {
        this.coverPhoto = coverPhoto;
        this.profilePhoto = profilePhoto;
        this.email = email;
        this.password = password;
        this.search = search;
        this.userId = userId;
        this.name = name;
        this.onlineStatus = onlineStatus;
        this.profession= profession;
        this.followerCount=followerCount;
        this.publicKey = publicKey;
    }

    public String getCoverPhoto() {
        return coverPhoto;
    }

    public void setCoverPhoto(String coverPhoto) {
        this.coverPhoto = coverPhoto;
    }

    public String getProfilePhoto() {
        return profilePhoto;
    }

    public void setProfilePhoto(String profilePhoto) {
        this.profilePhoto = profilePhoto;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }


    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOnlineStatus() {
        return onlineStatus;
    }

    public void setOnlineStatus(String onlineStatus) {
        this.onlineStatus = onlineStatus;
    }

    public String getProfession() {
        return profession;
    }

    public void setProfession(String profession) {
        this.profession = profession;
    }

    public int getFollowerCount() {
        return followerCount;
    }

    public void setFollowerCount(int followerCount) {
        this.followerCount = followerCount;
    }

    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }

}
