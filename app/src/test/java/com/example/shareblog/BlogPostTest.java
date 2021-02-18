package com.example.shareblog;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BlogPostTest {


    private String user_id = "user_id";
    private String image_url = "image_url";
    private String desc = "desc";
    private String image_thumb = "image_thumb";

    // Create a constructor in the blog post before starting the test.
    BlogPost blogPost = new BlogPost(user_id, image_url, desc, image_thumb);

    @Before
    public void setUp() throws Exception {
        BlogPost blogPost = new BlogPost();
    }

    @Test
    public void getUser_id() {
        assertEquals(blogPost.getUser_id(), "user_id");
    }

    @Test
    public void getImage_url() {
        assertEquals(blogPost.getImage_url(), "image_url");
    }

    @Test
    public void getDesc() {
        assertEquals(blogPost.getDesc(), "desc");
    }

    @Test
    public void getImage_thumb() {
        assertEquals(blogPost.getImage_thumb(), "image_thumb");
    }

}