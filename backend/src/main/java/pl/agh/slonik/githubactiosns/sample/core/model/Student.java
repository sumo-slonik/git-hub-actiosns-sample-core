package pl.agh.slonik.githubactiosns.sample.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@ToString
public class Student {
    private String name;
    private String Surname;
    private String id;
}
