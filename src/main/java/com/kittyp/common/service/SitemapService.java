package com.kittyp.common.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.kittyp.article.entity.Article;
import com.kittyp.article.enums.ArticleStatus;
import com.kittyp.article.repositry.ArticleRepository;
import com.kittyp.product.entity.Product;
import com.kittyp.product.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SitemapService {

    private final ProductRepository productRepository;
    private final ArticleRepository articleRepository;

    public record SitemapUrl(String loc, String lastmod, String changefreq, double priority) {}

    public List<SitemapUrl> generateSitemapUrls() {
        List<SitemapUrl> urls = new ArrayList<>();
        String currentDate = LocalDate.now().format(DateTimeFormatter.ISO_DATE);

        // Static pages with proper priorities and change frequencies
        urls.addAll(getStaticPages(currentDate));
        
        // Dynamic product pages
        urls.addAll(getProductPages());
        
        // Dynamic article pages
        urls.addAll(getArticlePages());

        return urls;
    }

    private List<SitemapUrl> getStaticPages(String currentDate) {
        String base = "https://www.kittyp.in";
        return List.of(
            // Main pages
            new SitemapUrl(base + "/", currentDate, "daily", 1.0),
            new SitemapUrl(base + "/about", currentDate, "monthly", 0.8),
            new SitemapUrl(base + "/articles", currentDate, "weekly", 0.7),
            new SitemapUrl(base + "/contact", currentDate, "monthly", 0.6),

            // Get started
            new SitemapUrl(base + "/login", currentDate, "monthly", 0.5),
            new SitemapUrl(base + "/signup", currentDate, "monthly", 0.5),
            new SitemapUrl(base + "/signup/parent", currentDate, "monthly", 0.5),
            new SitemapUrl(base + "/signup/doctor", currentDate, "monthly", 0.5),
            new SitemapUrl(base + "/signup/clinic-admin", currentDate, "monthly", 0.5),
            new SitemapUrl(base + "/forgot-password", currentDate, "monthly", 0.4),

            // Legal & utility
            new SitemapUrl(base + "/privacy", currentDate, "yearly", 0.3),
            new SitemapUrl(base + "/terms", currentDate, "yearly", 0.3),
            new SitemapUrl(base + "/sitemap", currentDate, "monthly", 0.4),
            new SitemapUrl(base + "/sitemap.xml", currentDate, "weekly", 0.5)
        );
    }

    private List<SitemapUrl> getProductPages() {
        List<SitemapUrl> productUrls = new ArrayList<>();
        String currentDate = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        
        try {
            // Use findAll() and filter for active products
            List<Product> allProducts = productRepository.findAll();
            List<Product> activeProducts = allProducts.stream()
                .filter(product -> Boolean.TRUE.equals(product.getIsActive()))
                .toList();
            
            for (Product product : activeProducts) {
                String lastmod = product.getUpdatedAt() != null 
                    ? product.getUpdatedAt().toLocalDate().format(DateTimeFormatter.ISO_DATE)
                    : currentDate;
                
                productUrls.add(new SitemapUrl(
                    "https://www.kittyp.in/product/" + product.getUuid(),
                    lastmod,
                    "weekly",
                    0.8
                ));
            }
        } catch (Exception e) {
            // Log error but don't fail the entire sitemap
            System.err.println("Error fetching products for sitemap: " + e.getMessage());
        }
        
        return productUrls;
    }

    private List<SitemapUrl> getArticlePages() {
        List<SitemapUrl> articleUrls = new ArrayList<>();
        String currentDate = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        
        try {
            // Use findAll() and filter for published articles
            List<Article> allArticles = articleRepository.findAll();
            List<Article> publishedArticles = allArticles.stream()
                .filter(article -> ArticleStatus.PUBLISHED.equals(article.getStatus()))
                .toList();
            
            for (Article article : publishedArticles) {
                String lastmod = article.getUpdatedAt() != null 
                    ? article.getUpdatedAt().toLocalDate().format(DateTimeFormatter.ISO_DATE)
                    : currentDate;
                
                articleUrls.add(new SitemapUrl(
                    "https://www.kittyp.in/article/" + article.getSlug(),
                    lastmod,
                    "monthly",
                    0.7
                ));
            }
        } catch (Exception e) {
            // Log error but don't fail the entire sitemap
            System.err.println("Error fetching articles for sitemap: " + e.getMessage());
        }
        
        return articleUrls;
    }
} 