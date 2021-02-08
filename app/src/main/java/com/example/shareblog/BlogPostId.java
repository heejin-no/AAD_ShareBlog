package com.example.shareblog;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.Exclude;

//import com.google.firebase.database.Exclude;

public class BlogPostId {

    @Exclude
    public String BlogPostId;

    public <T extends BlogPostId> T withId(@NonNull final String id){
        this.BlogPostId = id;
        return (T) this;
    }

}
