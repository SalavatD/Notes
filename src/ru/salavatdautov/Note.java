package ru.salavatdautov;

import java.util.Date;

public class Note implements Comparable<Note> {
    public Date date;
    public byte[] title;
    public byte[] body;

    @Override
    public int compareTo(Note note) {
        return this.date.compareTo(note.date);
    }
}
