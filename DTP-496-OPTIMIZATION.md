# DTP-496: Schema Management Validation Pipeline Optimization

## Overview
This optimization addresses the performance issues in the databricks-schema-management API validation pipeline by switching from full repository cloning to PR diff-based validation.

## Problem Statement
The validation pipeline for the databricks-schema-management API was cloning the full schema-management repository, which contained all YAML definitions. This full clone increased pipeline latency and compute overhead.

## Solution
The solution switches the pipeline to operate on PR diffs: retrieve only the files changed in the PR and run validation on those, eliminating the need for a full repository clone.

## Changes Made

### 1. New Services Added

#### PRDiffValidationService
- **File**: `src/main/java/ai/commerceiq/schemamanagement/service/PRDiffValidationService.java`
- **Purpose**: Handles validation using PR diff approach
- **Key Features**:
  - Processes only changed files from PR diff
  - Maintains same validation logic as original service
  - Optimized for performance

#### PRDiffRepoService
- **File**: `src/main/java/ai/commerceiq/schemamanagement/service/PRDiffRepoService.java`
- **Purpose**: Manages PR diff file retrieval and checksum generation
- **Key Features**:
  - Uses `git diff` to get only changed files
  - Filters for YAML files in the DSM folder path
  - Generates checksums only for changed files
  - Fallback to full repository processing if PR diff fails

### 2. Updated Services

#### ValidationService
- **File**: `src/main/java/ai/commerceiq/schemamanagement/service/ValidationService.java`
- **Changes**:
  - Added new method `validateFiles(String branchName, String userName, boolean usePRDiff, String prNumber)`
  - Maintains backward compatibility with existing `validateFiles(String branchName, String userName)` method
  - Delegates to appropriate validation approach based on parameters

#### SchemaManagementController
- **File**: `src/main/java/ai/commerceiq/schemamanagement/controller/SchemaManagementController.java`
- **Changes**:
  - Added `/validate-optimized` endpoint for optimized validation
  - Added `/validate-pr-diff` endpoint for PR-specific validation
  - Maintains existing `/validate` endpoint for backward compatibility
  - Added proper error handling and logging

### 3. Updated Pipeline Configuration

#### bitbucket-pipelines.yml
- **File**: `bitbucket-pipelines.yml`
- **Changes**:
  - Added new `optimizedValidationTest` step definition
  - Updated all pipeline branches to use optimized validation
  - Added PR-specific validation logic using `$BITBUCKET_PR_ID`
  - Maintains fallback to branch-based validation when PR ID is not available

## API Endpoints

### New Endpoints

#### 1. `/schemaManagement/v1/validate-optimized`
- **Method**: GET
- **Parameters**:
  - `branchName` (required): The branch to validate
  - `userName` (required): User performing validation
  - `usePRDiff` (optional, default: true): Whether to use PR diff approach
  - `prNumber` (optional): PR number for diff-based validation
- **Description**: Optimized validation endpoint with configurable approach

#### 2. `/schemaManagement/v1/validate-pr-diff`
- **Method**: GET
- **Parameters**:
  - `branchName` (required): The branch to validate
  - `userName` (required): User performing validation
  - `prNumber` (required): PR number for diff-based validation
- **Description**: PR-specific validation endpoint

### Existing Endpoints
- `/schemaManagement/v1/validate` - Maintained for backward compatibility

## Performance Benefits

### Before Optimization
- Full repository clone (~all YAML files)
- Process all files in repository
- Higher latency and compute overhead
- Longer pipeline execution time

### After Optimization
- PR diff-based file retrieval (~only changed files)
- Process only changed YAML files
- Reduced latency and compute overhead
- Faster pipeline execution time

## Implementation Details

### Git Diff Strategy
The optimization uses `git diff --name-only baseBranch...targetBranch` to identify changed files:
1. Clone repository (minimal, only for git operations)
2. Run git diff to get changed files
3. Filter for YAML files in DSM folder path
4. Generate checksums only for changed files
5. Process validation for changed files only

### Fallback Mechanism
If PR diff fails or PR number is not available:
1. Falls back to branch-based diff
2. If that fails, falls back to full repository processing
3. Ensures validation always completes

### Error Handling
- Comprehensive logging for debugging
- Graceful fallback mechanisms
- Proper error responses in API endpoints

## Testing

### Unit Tests
- Test PR diff file retrieval
- Test checksum generation for changed files
- Test validation logic with PR diff approach
- Test fallback mechanisms

### Integration Tests
- Test API endpoints with PR diff validation
- Test pipeline integration
- Test error scenarios and fallbacks

## Deployment

### Backward Compatibility
- All existing API endpoints maintained
- Existing pipeline configurations work unchanged
- Gradual migration to optimized approach

### Configuration
- New pipeline steps use optimized validation by default
- Can be disabled by setting `usePRDiff=false`
- PR-specific validation available when PR ID is provided

## Monitoring

### Metrics to Track
- Validation execution time
- Number of files processed
- PR diff success/failure rates
- Fallback usage frequency

### Logging
- Detailed logs for PR diff operations
- Performance metrics logging
- Error tracking and debugging information

## Future Enhancements

### Potential Improvements
1. **Bitbucket API Integration**: Direct API calls to get PR diff files
2. **Caching**: Cache PR diff results for repeated validations
3. **Parallel Processing**: Process multiple changed files in parallel
4. **Incremental Validation**: Only validate files that haven't been validated recently

### Configuration Options
1. **Configurable Base Branch**: Allow different base branches for diff
2. **File Pattern Filtering**: More sophisticated file filtering
3. **Validation Rules**: Configurable validation rules per file type

## Rollback Plan

If issues arise with the optimized approach:
1. Revert pipeline configuration to use original validation
2. Disable optimized endpoints
3. Use original `validateFiles` method
4. Full repository cloning will resume

## Conclusion

This optimization significantly improves the validation pipeline performance by:
- Reducing processing time through PR diff-based file retrieval
- Maintaining all existing functionality and API compatibility
- Providing robust fallback mechanisms
- Enabling better resource utilization

The changes are designed to be backward compatible and can be gradually adopted across different environments.