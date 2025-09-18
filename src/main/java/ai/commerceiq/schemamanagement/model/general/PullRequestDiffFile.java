package ai.commerceiq.schemamanagement.model.general;

import lombok.Data;

/**
 * Model representing a file that was changed in a Pull Request.
 */
@Data
public class PullRequestDiffFile {
    
    /**
     * The file path relative to the repository root.
     */
    private String filePath;
    
    /**
     * The status of the file change (added, modified, removed).
     */
    private String status;
    
    /**
     * Number of lines added (optional).
     */
    private Integer linesAdded;
    
    /**
     * Number of lines removed (optional).
     */
    private Integer linesRemoved;
    
    /**
     * The content of the file (if available).
     */
    private String content;
    
    /**
     * The checksum/hash of the file content.
     */
    private String checksum;
}