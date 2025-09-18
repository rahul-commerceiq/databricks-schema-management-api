#!/usr/bin/env python3
"""
DTP-496: Validation Report Generator

Generates human-readable validation reports from JSON validation results.

Author: AI Assistant
Date: 2025-09-18
Jira Ticket: DTP-496
"""

import json
import argparse
import sys
from datetime import datetime
from pathlib import Path

def generate_validation_report(input_file: str, output_file: str):
    """Generate human-readable validation report"""
    
    try:
        with open(input_file, 'r') as f:
            data = json.load(f)
    except FileNotFoundError:
        print(f"Error: Input file {input_file} not found")
        sys.exit(1)
    except json.JSONDecodeError as e:
        print(f"Error: Invalid JSON in {input_file}: {e}")
        sys.exit(1)
    
    # Generate report
    report_lines = []
    report_lines.append("=" * 80)
    report_lines.append("DATABRICKS SCHEMA MANAGEMENT - VALIDATION REPORT")
    report_lines.append("=" * 80)
    report_lines.append(f"Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    report_lines.append(f"Total Files: {data.get('total_files', 0)}")
    report_lines.append(f"Passed: {data.get('passed_files', 0)}")
    report_lines.append(f"Failed: {data.get('failed_files', 0)}")
    report_lines.append("")
    
    # Overall status
    if data.get('failed_files', 0) == 0:
        report_lines.append("✅ ALL VALIDATIONS PASSED")
        report_lines.append("")
    else:
        report_lines.append("❌ SOME VALIDATIONS FAILED")
        report_lines.append("")
    
    # Detailed results
    report_lines.append("DETAILED RESULTS:")
    report_lines.append("-" * 40)
    
    for result in data.get('results', []):
        file_path = result.get('file_path', 'Unknown')
        yaml_valid = result.get('yaml_valid', False)
        sql_valid = result.get('sql_valid', False)
        errors = result.get('errors', [])
        warnings = result.get('warnings', [])
        
        # File status
        if yaml_valid and sql_valid:
            status = "✅ PASSED"
        else:
            status = "❌ FAILED"
        
        report_lines.append(f"\nFile: {file_path}")
        report_lines.append(f"Status: {status}")
        report_lines.append(f"YAML Valid: {'Yes' if yaml_valid else 'No'}")
        report_lines.append(f"SQL Valid: {'Yes' if sql_valid else 'No'}")
        
        # Errors
        if errors:
            report_lines.append("Errors:")
            for error in errors:
                report_lines.append(f"  • {error}")
        
        # Warnings
        if warnings:
            report_lines.append("Warnings:")
            for warning in warnings:
                report_lines.append(f"  ⚠ {warning}")
    
    # Summary
    report_lines.append("\n" + "=" * 80)
    report_lines.append("SUMMARY")
    report_lines.append("=" * 80)
    
    total_files = data.get('total_files', 0)
    passed_files = data.get('passed_files', 0)
    failed_files = data.get('failed_files', 0)
    
    if total_files > 0:
        success_rate = (passed_files / total_files) * 100
        report_lines.append(f"Success Rate: {success_rate:.1f}%")
    
    if failed_files > 0:
        report_lines.append(f"Failed Files: {failed_files}")
        report_lines.append("\nPlease review the errors above and fix them before proceeding.")
    else:
        report_lines.append("All validations passed successfully!")
        report_lines.append("The schema changes are ready for migration.")
    
    # Write report
    report_content = "\n".join(report_lines)
    
    with open(output_file, 'w') as f:
        f.write(report_content)
    
    print(f"Validation report generated: {output_file}")
    
    # Print summary to console
    print("\n" + "=" * 50)
    print("VALIDATION SUMMARY")
    print("=" * 50)
    print(f"Total Files: {total_files}")
    print(f"Passed: {passed_files}")
    print(f"Failed: {failed_files}")
    
    if failed_files > 0:
        print("\n❌ Validation failed. Please check the report for details.")
        sys.exit(1)
    else:
        print("\n✅ All validations passed!")
        sys.exit(0)

def main():
    """Main entry point"""
    parser = argparse.ArgumentParser(description='Generate validation report')
    parser.add_argument('--input', required=True, help='Input JSON file')
    parser.add_argument('--output', required=True, help='Output report file')
    
    args = parser.parse_args()
    
    if not Path(args.input).exists():
        print(f"Error: Input file {args.input} does not exist")
        sys.exit(1)
    
    generate_validation_report(args.input, args.output)

if __name__ == '__main__':
    main()