package com.kittyp.common.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kittyp.common.constants.ApiUrl;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping(ApiUrl.BASE_URL)
public class SitemapController {

    private record Page(String loc, String lastmod) {}

    @GetMapping(value = ApiUrl.SITEMAP, produces = MediaType.APPLICATION_XML_VALUE)
    public void getSitemap(HttpServletResponse response) throws IOException {
        List<Page> pages = List.of(
            new Page("https://kittyp.in/", LocalDate.now().toString()),
            new Page("https://kittyp.in/about", "2025-05-20"),
            new Page("https://kittyp.in/contact", "2025-05-15"),
            new Page("https://kittyp.in/products", "2025-05-15"),
            new Page("https://kittyp.in/how-to-use", "2025-05-15"),
            new Page("https://kittyp.in/articles", "2025-05-15"),
            new Page("https://kittyp.in/login", "2025-05-15"),
            new Page("https://kittyp.in/signup", "2025-05-15"),
            new Page("https://kittyp.in/forgot-password", "2025-05-15"),
            new Page("https://kittyp.in/privacy", "2025-05-15"),
            new Page("https://kittyp.in/why-eco-litter", "2025-05-15"),
            new Page("https://kittyp.in/terms", "2025-05-15"),
            new Page("https://kittyp.in/sitemap", "2025-05-15"),
            new Page("https://kittyp.in/profile", "2025-05-15"),
            new Page("https://kittyp.in/orders", "2025-05-15"),
            new Page("https://kittyp.in/cart", "2025-05-15"),
            new Page("https://kittyp.in/checkout", "2025-05-15"),
            new Page("https://kittyp.in/cart", "2025-05-15")
        );

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");

        for (Page page : pages) {
            xml.append("<url>")
                .append("<loc>").append(page.loc()).append("</loc>")
                .append("<lastmod>").append(page.lastmod()).append("</lastmod>")
                .append("<changefreq>monthly</changefreq>")
                .append("<priority>0.8</priority>")
                .append("</url>");
        }

        xml.append("</urlset>");

        response.getWriter().write(xml.toString());
    }
}

