package com.devcompass.ai.source;

import com.devcompass.ai.model.Document;
import com.devcompass.ai.model.SourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
public class NotionKnowledgeSource implements KnowledgeSource {

    private static final Logger log = LoggerFactory.getLogger(NotionKnowledgeSource.class);

    private String apiKey;
    private String mainPageId;
    private final RestTemplate restTemplate;

    public NotionKnowledgeSource() {
        this("demo-key", "");
    }

    @Autowired
    public NotionKnowledgeSource(
        @Value("${devcompass.sources.notion.api-key:demo-key}") String apiKey,
        @Value("${devcompass.sources.notion.main-page-id:}") String mainPageId
    ) {
        this.apiKey = apiKey;
        this.mainPageId = mainPageId;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Update Notion credentials at runtime for per-account sync.
     */
    public void updateConfig(String apiToken, String pageOrDatabaseId) {
        if (apiToken != null && !apiToken.isBlank()) {
            this.apiKey = apiToken;
        }
        if (pageOrDatabaseId != null && !pageOrDatabaseId.isBlank()) {
            this.mainPageId = pageOrDatabaseId;
        }
    }

    public Optional<String> validateConfig() {
        if (apiKey == null || apiKey.isBlank() || apiKey.equalsIgnoreCase("demo-key")) {
            return Optional.of("Notion API Key is missing or unconfigured.");
        }
        if (mainPageId == null || mainPageId.isBlank() || mainPageId.equalsIgnoreCase("demo-key")) {
            return Optional.of("Notion Main Page ID is unconfigured.");
        }
        return Optional.empty();
    }


    @Override
    public List<Document> sync() {
        if (apiKey != null && !apiKey.isBlank() && !apiKey.equalsIgnoreCase("demo-key")) {
            try {
                List<Document> liveDocs = fetchLiveNotionDocuments();
                if (liveDocs != null && !liveDocs.isEmpty()) {
                    log.info("Returning {} live Notion documents originating from configured main page ID.", liveDocs.size());
                    return liveDocs;
                }
            } catch (Exception e) {
                log.error("Failed to sync live Notion documents: {}", e.getMessage());
            }
        }

        log.warn("Notion API key not configured or live fetch returned empty. Returning empty documents list.");
        return new ArrayList<>();
    }

    public Document fetchSinglePageDocument(String rawPageId) {
        if (apiKey == null || apiKey.isBlank() || apiKey.equalsIgnoreCase("demo-key")) {
            return null;
        }

        String pageId = formatNotionUuid(rawPageId);
        HttpHeaders headers = createNotionHeaders();
        String pageUrl = "https://api.notion.com/v1/pages/" + pageId;
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(pageUrl, HttpMethod.GET, requestEntity, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map page = response.getBody();
                String title = extractPageTitle(page);
                String content = fetchPageBlockContent(pageId, headers, new HashSet<>(), 0);
                String lastEditedTime = page.get("last_edited_time") != null ? page.get("last_edited_time").toString() : "";

                return new Document(
                    UUID.randomUUID().toString(),
                    title,
                    "notion-page-" + pageId,
                    SourceType.NOTION,
                    content,
                    Map.of(
                        "notionPageId", pageId,
                        "url", page.get("url") != null ? page.get("url").toString() : "",
                        "lastEditedTime", lastEditedTime,
                        "source", "Live Notion REST API Webhook"
                    )
                );
            }
        } catch (Exception e) {
            log.error("Error fetching single Notion page {}: {}", pageId, e.getMessage());
        }

        return null;
    }

    private List<Document> fetchLiveNotionDocuments() {
        HttpHeaders headers = createNotionHeaders();
        List<Document> documents = new ArrayList<>();
        Set<String> visitedPages = new HashSet<>();

        if (mainPageId != null && !mainPageId.isBlank() && !mainPageId.equalsIgnoreCase("demo-key")) {
            String formattedRootId = formatNotionUuid(mainPageId);
            log.info("Confining Notion crawl strictly to configured Main Page ID: {} and its linked child pages...", formattedRootId);
            crawlNotionPageRecursively(formattedRootId, headers, visitedPages, documents);
        } else {
            log.warn("Option A Active: 'devcompass.sources.notion.main-page-id' is not configured in application.yml. Specify page ID to crawl.");
        }

        log.info("Successfully crawled {} documents strictly from configured Notion main page hierarchy.", documents.size());
        return documents;
    }

    private void crawlNotionPageRecursively(String pageId, HttpHeaders headers, Set<String> visitedPages, List<Document> documents) {
        if (!visitedPages.add(pageId)) {
            return; // Prevent duplicate visits or circular page links
        }

        String pageUrl = "https://api.notion.com/v1/pages/" + pageId;
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(pageUrl, HttpMethod.GET, requestEntity, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map page = response.getBody();
                String title = extractPageTitle(page);
                log.info("Crawling Notion page: '{}' (ID: {})", title, pageId);

                String content = fetchPageBlockContent(pageId, headers, visitedPages, 0);
                String lastEditedTime = page.get("last_edited_time") != null ? page.get("last_edited_time").toString() : "";

                if (content != null && !content.isBlank()) {
                    documents.add(new Document(
                        UUID.randomUUID().toString(),
                        title,
                        "notion-page-" + pageId,
                        SourceType.NOTION,
                        content,
                        Map.of(
                            "notionPageId", pageId,
                            "url", page.get("url") != null ? page.get("url").toString() : "",
                            "lastEditedTime", lastEditedTime,
                            "source", "Live Notion REST API Main Traversal"
                        )
                    ));
                }
            }
        } catch (Exception e) {
            log.error("Failed to crawl Notion page {}: {}", pageId, e.getMessage());
        }
    }

    private String fetchPageBlockContent(String blockId, HttpHeaders headers, Set<String> visitedPages, int depth) {
        if (depth > 5) return ""; // Depth limit for nested blocks

        String blocksUrl = "https://api.notion.com/v1/blocks/" + blockId + "/children?page_size=100";
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(blocksUrl, HttpMethod.GET, requestEntity, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map> blocks = (List<Map>) response.getBody().get("results");
                if (blocks != null) {
                    StringBuilder markdown = new StringBuilder();
                    for (Map block : blocks) {
                        String blockType = (String) block.get("type");
                        Boolean hasChildren = (Boolean) block.get("has_children");
                        String childBlockId = (String) block.get("id");

                        if ("child_page".equals(blockType)) {
                            Map childPageDetails = (Map) block.get("child_page");
                            String childTitle = childPageDetails != null ? (String) childPageDetails.get("title") : "Child Page";
                            markdown.append("\n### [Child Page: ").append(childTitle).append("]\n");

                            // Recurse into child page link under main page
                            if (childBlockId != null && visitedPages != null && !visitedPages.contains(childBlockId)) {
                                log.info("Following child page link: '{}' (ID: {})", childTitle, childBlockId);
                                String childContent = fetchPageBlockContent(childBlockId, headers, visitedPages, depth + 1);
                                markdown.append(childContent).append("\n");
                            }
                        } else if ("link_to_page".equals(blockType)) {
                            Map linkDetails = (Map) block.get("link_to_page");
                            if (linkDetails != null) {
                                String linkedPageId = linkDetails.get("page_id") != null ? linkDetails.get("page_id").toString() : null;
                                if (linkedPageId != null && visitedPages != null && !visitedPages.contains(linkedPageId)) {
                                    log.info("Following linked page ID: {}", linkedPageId);
                                    String linkedContent = fetchPageBlockContent(linkedPageId, headers, visitedPages, depth + 1);
                                    if (!linkedContent.isBlank()) {
                                        markdown.append("\n### [Linked Page Content]\n").append(linkedContent).append("\n");
                                    }
                                }
                            }
                        } else if (blockType != null && block.containsKey(blockType)) {
                            Map blockDetails = (Map) block.get(blockType);
                            String text = extractRichText(blockDetails);
                            if (!text.isBlank()) {
                                switch (blockType) {
                                    case "heading_1" -> markdown.append("# ").append(text).append("\n\n");
                                    case "heading_2" -> markdown.append("## ").append(text).append("\n\n");
                                    case "heading_3" -> markdown.append("### ").append(text).append("\n\n");
                                    case "bulleted_list_item" -> markdown.append("- ").append(text).append("\n");
                                    case "numbered_list_item" -> markdown.append("1. ").append(text).append("\n");
                                    case "toggle", "callout", "quote" -> markdown.append("> ").append(text).append("\n\n");
                                    case "code" -> markdown.append("```\n").append(text).append("\n```\n\n");
                                    default -> markdown.append(text).append("\n\n");
                                }
                            }

                            // Recurse into sub-blocks (e.g., toggles, nested lists)
                            if (Boolean.TRUE.equals(hasChildren) && childBlockId != null) {
                                String nestedContent = fetchPageBlockContent(childBlockId, headers, visitedPages, depth + 1);
                                if (!nestedContent.isBlank()) {
                                    markdown.append(nestedContent);
                                }
                            }
                        }
                    }
                    return markdown.toString();
                }
            }
        } catch (Exception e) {
            log.debug("Notice reading block children for {}: {}", blockId, e.getMessage());
        }

        return "";
    }

    private String extractPageTitle(Map page) {
        if (page != null && page.get("properties") instanceof Map properties) {
            for (Object key : properties.keySet()) {
                if (properties.get(key) instanceof Map propMap && "title".equals(propMap.get("type"))) {
                    return extractRichText(propMap);
                }
            }
        }
        return "Untitled Notion Page (" + (page != null ? page.get("id") : "unknown") + ")";
    }

    private String extractRichText(Map parentMap) {
        if (parentMap != null && parentMap.get("rich_text") instanceof List richTextList && !richTextList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Object item : richTextList) {
                if (item instanceof Map itemMap && itemMap.get("plain_text") != null) {
                    sb.append(itemMap.get("plain_text"));
                }
            }
            return sb.toString();
        } else if (parentMap != null && parentMap.get("title") instanceof List titleList && !titleList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Object item : titleList) {
                if (item instanceof Map itemMap && itemMap.get("plain_text") != null) {
                    sb.append(itemMap.get("plain_text"));
                }
            }
            return sb.toString();
        }
        return "";
    }

    private String formatNotionUuid(String rawId) {
        if (rawId == null) return "";
        String clean = rawId.replace("-", "").trim();
        if (clean.length() == 32) {
            return clean.substring(0, 8) + "-" + clean.substring(8, 12) + "-" +
                   clean.substring(12, 16) + "-" + clean.substring(16, 20) + "-" +
                   clean.substring(20);
        }
        return rawId;
    }

    private HttpHeaders createNotionHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("Notion-Version", "2022-06-28");
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Override
    public SourceType type() {
        return SourceType.NOTION;
    }

    @Override
    public String sourceName() {
        return "Notion Engineering Knowledge Workspace";
    }

    @Override
    public boolean isHealthy() {
        return apiKey != null && !apiKey.isBlank();
    }
}
