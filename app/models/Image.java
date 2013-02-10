package models;

import play.db.ebean.Model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;


@SuppressWarnings("serial")
@Entity
public class Image extends Model {

    @Id
    public Long id;

    @Column(unique = true)
    public String uri;

    @Column()
    @Lob
    public byte[] image;


    // -- Queries (long id, image.class)
    public static Model.Finder<Long, Image> find = new Model.Finder<Long, Image>(Long.class, Image.class);

    public static Image findByUri(String uri) {
        return find.where().eq("uri", uri).findUnique();
    }
}
