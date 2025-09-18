#!/usr/bin/env python3
"""
DTP-496: PR Diff Validation for Databricks Schema Management API

This script optimizes the validation pipeline by processing only the files
changed in a PR instead of cloning the entire repository.

Author: AI Assistant
Date: 2025-09-18
Jira Ticket: DTP-496
"""

import os
import sys
import json
import yaml
import requests
import argparse
import logging
from typing import List, Dict, Any, Optional
from dataclasses import dataclass
from pathlib import Path

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

@dataclass
class ValidationResult:
    """Data class for validation results"""
    file_path: str
    yaml_valid: bool
    sql_valid: bool
    errors: List[str]
    warnings: List[str] = None

class BitbucketAPIClient:
    """Client for interacting with Bitbucket API"""
    
    def __init__(self, workspace: str, repo_slug: str, access_token: str):
        self.workspace = workspace
        self.repo_slug = repo_slug
        self.access_token = access_token
        self.base_url = f"https://api.bitbucket.org/2.0/repositories/{workspace}/{repo_slug}"
        self.headers = {
            'Authorization': f'Bearer {access_token}',
            'Accept': 'application/json'
        }
    
    def get_pr_diff(self, pr_id: int) -> str:
        """Get the diff for a pull request"""
        diff_url = f"{self.base_url}/pullrequests/{pr_id}/diff"
        
        try:
            response = requests.get(diff_url, headers=self.headers, timeout=30)
            response.raise_for_status()
            return response.text
        except requests.exceptions.RequestException as e:
            logger.error(f"Failed to fetch PR diff: {e}")
            raise
    
    def get_file_content(self, file_path: str, commit_hash: str) -> str:
        """Get the content of a specific file at a given commit"""
        file_url = f"{self.base_url}/src/{commit_hash}/{file_path}"
        
        try:
            response = requests.get(file_url, headers=self.headers, timeout=30)
            response.raise_for_status()
            return response.text
        except requests.exceptions.RequestException as e:
            logger.error(f"Failed to fetch file {file_path}: {e}")
            raise
    
    def get_pr_info(self, pr_id: int) -> Dict[str, Any]:
        """Get pull request information"""
        pr_url = f"{self.base_url}/pullrequests/{pr_id}"
        
        try:
            response = requests.get(pr_url, headers=self.headers, timeout=30)
            response.raise_for_status()
            return response.json()
        except requests.exceptions.RequestException as e:
            logger.error(f"Failed to fetch PR info: {e}")
            raise

class DiffParser:
    """Parser for Bitbucket diff content"""
    
    @staticmethod
    def extract_changed_yaml_files(diff_content: str) -> List[str]:
        """Extract YAML files that were changed in the diff"""
        yaml_files = []
        current_file = None
        
        for line in diff_content.split('\n'):
            # Check for file header (diff --git a/path b/path)
            if line.startswith('diff --git'):
                # Extract file path from diff header
                parts = line.split()
                if len(parts) >= 4:
                    file_path = parts[2][2:]  # Remove 'a/' prefix
                    if file_path.endswith(('.yaml', '.yml')):
                        yaml_files.append(file_path)
                        current_file = file_path
            # Check for new file indicator
            elif line.startswith('new file mode') and current_file:
                if current_file.endswith(('.yaml', '.yml')):
                    yaml_files.append(current_file)
        
        return list(set(yaml_files))  # Remove duplicates

class YAMLValidator:
    """Validator for YAML schema management files"""
    
    def __init__(self):
        self.valid_ddl_types = {
            'create_table', 'drop_table', 'alter_table', 
            'create_view', 'drop_view', 'create_schema', 'create_function'
        }
    
    def validate_yaml_structure(self, content: str) -> Dict[str, Any]:
        """Validate YAML structure and schema"""
        errors = []
        warnings = []
        
        try:
            # Parse YAML
            yaml_data = yaml.safe_load(content)
            
            if not isinstance(yaml_data, dict):
                errors.append("YAML must contain a dictionary at root level")
                return {'valid': False, 'errors': errors, 'warnings': warnings}
            
            # Validate DDLType
            if 'DDLType' not in yaml_data:
                errors.append("Missing required field: DDLType")
            elif yaml_data['DDLType'] not in self.valid_ddl_types:
                errors.append(f"Invalid DDLType: {yaml_data['DDLType']}. Must be one of {self.valid_ddl_types}")
            
            # Validate changeSets
            if 'changeSets' not in yaml_data:
                errors.append("Missing required field: changeSets")
            elif not isinstance(yaml_data['changeSets'], list):
                errors.append("changeSets must be a list")
            else:
                # Validate each changeSet
                for i, change_set in enumerate(yaml_data['changeSets']):
                    if not isinstance(change_set, dict):
                        errors.append(f"changeSet {i} must be a dictionary")
                        continue
                    
                    # Validate required fields based on DDLType
                    ddl_type = yaml_data.get('DDLType', '')
                    self._validate_change_set(change_set, ddl_type, i, errors, warnings)
            
        except yaml.YAMLError as e:
            errors.append(f"YAML parsing error: {str(e)}")
        except Exception as e:
            errors.append(f"Unexpected error: {str(e)}")
        
        return {
            'valid': len(errors) == 0,
            'errors': errors,
            'warnings': warnings
        }
    
    def _validate_change_set(self, change_set: Dict, ddl_type: str, index: int, 
                           errors: List[str], warnings: List[str]):
        """Validate individual change set based on DDL type"""
        # Common validations
        if 'id' not in change_set:
            errors.append(f"changeSet {index}: Missing required field 'id'")
        
        # DDL-specific validations
        if ddl_type == 'create_table':
            self._validate_create_table(change_set, index, errors, warnings)
        elif ddl_type == 'drop_table':
            self._validate_drop_table(change_set, index, errors, warnings)
        elif ddl_type == 'alter_table':
            self._validate_alter_table(change_set, index, errors, warnings)
        elif ddl_type == 'create_view':
            self._validate_create_view(change_set, index, errors, warnings)
        elif ddl_type == 'create_schema':
            self._validate_create_schema(change_set, index, errors, warnings)
        elif ddl_type == 'create_function':
            self._validate_create_function(change_set, index, errors, warnings)
    
    def _validate_create_table(self, change_set: Dict, index: int, 
                             errors: List[str], warnings: List[str]):
        """Validate create_table change set"""
        if 'tableName' not in change_set:
            errors.append(f"changeSet {index}: Missing required field 'tableName'")
        
        if 'columns' in change_set:
            if not isinstance(change_set['columns'], list):
                errors.append(f"changeSet {index}: 'columns' must be a list")
            else:
                for col_idx, column in enumerate(change_set['columns']):
                    if not isinstance(column, dict):
                        errors.append(f"changeSet {index}, column {col_idx}: Must be a dictionary")
                        continue
                    
                    if 'name' not in column:
                        errors.append(f"changeSet {index}, column {col_idx}: Missing 'name'")
                    if 'type' not in column:
                        errors.append(f"changeSet {index}, column {col_idx}: Missing 'type'")
    
    def _validate_drop_table(self, change_set: Dict, index: int, 
                           errors: List[str], warnings: List[str]):
        """Validate drop_table change set"""
        if 'tableName' not in change_set:
            errors.append(f"changeSet {index}: Missing required field 'tableName'")
    
    def _validate_alter_table(self, change_set: Dict, index: int, 
                            errors: List[str], warnings: List[str]):
        """Validate alter_table change set"""
        if 'tableName' not in change_set:
            errors.append(f"changeSet {index}: Missing required field 'tableName'")
        if 'alterationType' not in change_set:
            errors.append(f"changeSet {index}: Missing required field 'alterationType'")
    
    def _validate_create_view(self, change_set: Dict, index: int, 
                            errors: List[str], warnings: List[str]):
        """Validate create_view change set"""
        if 'viewStatement' not in change_set:
            errors.append(f"changeSet {index}: Missing required field 'viewStatement'")
    
    def _validate_create_schema(self, change_set: Dict, index: int, 
                              errors: List[str], warnings: List[str]):
        """Validate create_schema change set"""
        if 'schemaName' not in change_set:
            errors.append(f"changeSet {index}: Missing required field 'schemaName'")
    
    def _validate_create_function(self, change_set: Dict, index: int, 
                                errors: List[str], warnings: List[str]):
        """Validate create_function change set"""
        if 'functionDefinition' not in change_set:
            errors.append(f"changeSet {index}: Missing required field 'functionDefinition'")

class SQLValidator:
    """Validator for SQL queries in YAML files"""
    
    def __init__(self):
        # Basic SQL validation patterns
        self.dangerous_patterns = [
            'DROP DATABASE',
            'DROP SCHEMA',
            'TRUNCATE',
            'DELETE FROM',
            'UPDATE.*SET'
        ]
    
    def validate_sql_queries(self, content: str) -> Dict[str, Any]:
        """Validate SQL queries in the YAML content"""
        errors = []
        warnings = []
        
        try:
            yaml_data = yaml.safe_load(content)
            if not isinstance(yaml_data, dict):
                return {'valid': True, 'errors': [], 'warnings': []}
            
            # Extract SQL from various fields
            sql_fields = ['viewStatement', 'functionDefinition']
            for field in sql_fields:
                if field in yaml_data.get('changeSets', []):
                    for change_set in yaml_data['changeSets']:
                        if field in change_set:
                            sql_content = change_set[field]
                            self._validate_sql_content(sql_content, field, errors, warnings)
            
        except Exception as e:
            errors.append(f"SQL validation error: {str(e)}")
        
        return {
            'valid': len(errors) == 0,
            'errors': errors,
            'warnings': warnings
        }
    
    def _validate_sql_content(self, sql_content: str, field_name: str, 
                            errors: List[str], warnings: List[str]):
        """Validate SQL content for dangerous patterns"""
        if not sql_content:
            return
        
        sql_upper = sql_content.upper()
        
        for pattern in self.dangerous_patterns:
            if pattern in sql_upper:
                warnings.append(f"Potentially dangerous SQL pattern in {field_name}: {pattern}")

class PRDiffValidator:
    """Main validator class for PR diff validation"""
    
    def __init__(self, workspace: str, repo_slug: str, access_token: str):
        self.api_client = BitbucketAPIClient(workspace, repo_slug, access_token)
        self.yaml_validator = YAMLValidator()
        self.sql_validator = SQLValidator()
        self.diff_parser = DiffParser()
    
    def validate_pr_changes(self, pr_id: int) -> List[ValidationResult]:
        """Validate only the YAML files changed in the PR"""
        logger.info(f"Starting validation for PR #{pr_id}")
        
        try:
            # Get PR information
            pr_info = self.api_client.get_pr_info(pr_id)
            source_commit = pr_info['source']['commit']['hash']
            
            # Get PR diff
            diff_content = self.api_client.get_pr_diff(pr_id)
            
            # Extract changed YAML files
            changed_yaml_files = self.diff_parser.extract_changed_yaml_files(diff_content)
            logger.info(f"Found {len(changed_yaml_files)} changed YAML files: {changed_yaml_files}")
            
            if not changed_yaml_files:
                logger.info("No YAML files changed in this PR")
                return []
            
            # Validate each changed file
            validation_results = []
            for file_path in changed_yaml_files:
                logger.info(f"Validating file: {file_path}")
                
                try:
                    # Fetch file content
                    file_content = self.api_client.get_file_content(file_path, source_commit)
                    
                    # Validate YAML structure
                    yaml_validation = self.yaml_validator.validate_yaml_structure(file_content)
                    
                    # Validate SQL queries
                    sql_validation = self.sql_validator.validate_sql_queries(file_content)
                    
                    # Create validation result
                    result = ValidationResult(
                        file_path=file_path,
                        yaml_valid=yaml_validation['valid'],
                        sql_valid=sql_validation['valid'],
                        errors=yaml_validation['errors'] + sql_validation['errors'],
                        warnings=yaml_validation['warnings'] + sql_validation['warnings']
                    )
                    
                    validation_results.append(result)
                    
                    # Log validation status
                    if result.yaml_valid and result.sql_valid:
                        logger.info(f"✓ {file_path} - Validation passed")
                    else:
                        logger.error(f"✗ {file_path} - Validation failed")
                        for error in result.errors:
                            logger.error(f"  Error: {error}")
                
                except Exception as e:
                    logger.error(f"Failed to validate {file_path}: {e}")
                    validation_results.append(ValidationResult(
                        file_path=file_path,
                        yaml_valid=False,
                        sql_valid=False,
                        errors=[f"Validation failed: {str(e)}"]
                    ))
            
            return validation_results
            
        except Exception as e:
            logger.error(f"PR validation failed: {e}")
            raise

def generate_validation_report(results: List[ValidationResult], output_file: str):
    """Generate validation report in JSON format"""
    report = {
        'timestamp': str(Path().cwd()),
        'total_files': len(results),
        'passed_files': sum(1 for r in results if r.yaml_valid and r.sql_valid),
        'failed_files': sum(1 for r in results if not (r.yaml_valid and r.sql_valid)),
        'results': []
    }
    
    for result in results:
        report['results'].append({
            'file_path': result.file_path,
            'yaml_valid': result.yaml_valid,
            'sql_valid': result.sql_valid,
            'errors': result.errors,
            'warnings': result.warnings or []
        })
    
    with open(output_file, 'w') as f:
        json.dump(report, f, indent=2)
    
    logger.info(f"Validation report saved to: {output_file}")

def main():
    """Main entry point"""
    parser = argparse.ArgumentParser(description='Validate PR diff for schema management')
    parser.add_argument('--pr-id', type=int, required=True, help='Pull request ID')
    parser.add_argument('--workspace', required=True, help='Bitbucket workspace')
    parser.add_argument('--repo-slug', required=True, help='Repository slug')
    parser.add_argument('--access-token', required=True, help='Bitbucket access token')
    parser.add_argument('--output', default='validation_report.json', help='Output file for validation report')
    
    args = parser.parse_args()
    
    try:
        # Initialize validator
        validator = PRDiffValidator(args.workspace, args.repo_slug, args.access_token)
        
        # Validate PR changes
        results = validator.validate_pr_changes(args.pr_id)
        
        # Generate report
        generate_validation_report(results, args.output)
        
        # Determine exit code
        failed_files = [r for r in results if not (r.yaml_valid and r.sql_valid)]
        if failed_files:
            logger.error(f"Validation failed for {len(failed_files)} files")
            sys.exit(1)
        else:
            logger.info("All validations passed successfully")
            sys.exit(0)
            
    except Exception as e:
        logger.error(f"Validation process failed: {e}")
        sys.exit(1)

if __name__ == '__main__':
    main()