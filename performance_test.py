#!/usr/bin/env python3
"""
DTP-496: Performance Testing Script

Compares performance between full repository clone and PR diff validation approaches.

Author: AI Assistant
Date: 2025-09-18
Jira Ticket: DTP-496
"""

import time
import json
import argparse
import subprocess
import sys
from pathlib import Path
from typing import Dict, Any
import psutil
import os

class PerformanceMonitor:
    """Monitor system performance during validation"""
    
    def __init__(self):
        self.start_time = None
        self.start_memory = None
        self.start_cpu = None
        self.peak_memory = 0
        self.peak_cpu = 0
    
    def start(self):
        """Start monitoring"""
        self.start_time = time.time()
        self.start_memory = psutil.Process().memory_info().rss / 1024 / 1024  # MB
        self.start_cpu = psutil.cpu_percent()
    
    def stop(self):
        """Stop monitoring and return metrics"""
        end_time = time.time()
        end_memory = psutil.Process().memory_info().rss / 1024 / 1024  # MB
        end_cpu = psutil.cpu_percent()
        
        return {
            'duration_seconds': end_time - self.start_time,
            'memory_used_mb': end_memory - self.start_memory,
            'peak_memory_mb': self.peak_memory,
            'peak_cpu_percent': self.peak_cpu,
            'start_memory_mb': self.start_memory,
            'end_memory_mb': end_memory
        }
    
    def update_peak(self):
        """Update peak memory and CPU usage"""
        current_memory = psutil.Process().memory_info().rss / 1024 / 1024
        current_cpu = psutil.cpu_percent()
        
        self.peak_memory = max(self.peak_memory, current_memory)
        self.peak_cpu = max(self.peak_cpu, current_cpu)

def run_full_clone_validation(workspace: str, repo_slug: str, access_token: str) -> Dict[str, Any]:
    """Run validation using full repository clone approach"""
    print("Running full repository clone validation...")
    
    monitor = PerformanceMonitor()
    monitor.start()
    
    try:
        # Simulate full clone approach
        clone_cmd = [
            'git', 'clone', 
            f'https://{access_token}@bitbucket.org/{workspace}/{repo_slug}.git',
            'temp_repo'
        ]
        
        result = subprocess.run(clone_cmd, capture_output=True, text=True, timeout=300)
        
        if result.returncode != 0:
            raise Exception(f"Git clone failed: {result.stderr}")
        
        # Simulate validation of all YAML files
        validation_cmd = [
            'python', 'validate_all_yaml_files.py',
            '--workspace', workspace,
            '--repo-slug', repo_slug
        ]
        
        result = subprocess.run(validation_cmd, capture_output=True, text=True, timeout=600)
        
        metrics = monitor.stop()
        metrics['success'] = result.returncode == 0
        metrics['stdout'] = result.stdout
        metrics['stderr'] = result.stderr
        
        return metrics
        
    except subprocess.TimeoutExpired:
        metrics = monitor.stop()
        metrics['success'] = False
        metrics['error'] = 'Timeout exceeded'
        return metrics
    except Exception as e:
        metrics = monitor.stop()
        metrics['success'] = False
        metrics['error'] = str(e)
        return metrics
    finally:
        # Cleanup
        if Path('temp_repo').exists():
            subprocess.run(['rm', '-rf', 'temp_repo'], capture_output=True)

def run_pr_diff_validation(workspace: str, repo_slug: str, access_token: str, pr_id: int) -> Dict[str, Any]:
    """Run validation using PR diff approach"""
    print(f"Running PR diff validation for PR #{pr_id}...")
    
    monitor = PerformanceMonitor()
    monitor.start()
    
    try:
        # Run PR diff validation
        validation_cmd = [
            'python', 'validate_pr_diff.py',
            '--pr-id', str(pr_id),
            '--workspace', workspace,
            '--repo-slug', repo_slug,
            '--access-token', access_token,
            '--output', 'pr_validation_report.json'
        ]
        
        result = subprocess.run(validation_cmd, capture_output=True, text=True, timeout=300)
        
        metrics = monitor.stop()
        metrics['success'] = result.returncode == 0
        metrics['stdout'] = result.stdout
        metrics['stderr'] = result.stderr
        
        return metrics
        
    except subprocess.TimeoutExpired:
        metrics = monitor.stop()
        metrics['success'] = False
        metrics['error'] = 'Timeout exceeded'
        return metrics
    except Exception as e:
        metrics = monitor.stop()
        metrics['success'] = False
        metrics['error'] = str(e)
        return metrics

def compare_performance(full_clone_metrics: Dict[str, Any], pr_diff_metrics: Dict[str, Any]) -> Dict[str, Any]:
    """Compare performance metrics between approaches"""
    
    comparison = {
        'duration_improvement': {
            'full_clone_seconds': full_clone_metrics.get('duration_seconds', 0),
            'pr_diff_seconds': pr_diff_metrics.get('duration_seconds', 0),
            'improvement_percent': 0
        },
        'memory_improvement': {
            'full_clone_mb': full_clone_metrics.get('memory_used_mb', 0),
            'pr_diff_mb': pr_diff_metrics.get('memory_used_mb', 0),
            'improvement_percent': 0
        },
        'success_rates': {
            'full_clone_success': full_clone_metrics.get('success', False),
            'pr_diff_success': pr_diff_metrics.get('success', False)
        }
    }
    
    # Calculate improvements
    if full_clone_metrics.get('duration_seconds', 0) > 0:
        duration_improvement = (
            (full_clone_metrics['duration_seconds'] - pr_diff_metrics.get('duration_seconds', 0)) /
            full_clone_metrics['duration_seconds'] * 100
        )
        comparison['duration_improvement']['improvement_percent'] = duration_improvement
    
    if full_clone_metrics.get('memory_used_mb', 0) > 0:
        memory_improvement = (
            (full_clone_metrics['memory_used_mb'] - pr_diff_metrics.get('memory_used_mb', 0)) /
            full_clone_metrics['memory_used_mb'] * 100
        )
        comparison['memory_improvement']['improvement_percent'] = memory_improvement
    
    return comparison

def generate_performance_report(full_clone_metrics: Dict[str, Any], 
                              pr_diff_metrics: Dict[str, Any], 
                              comparison: Dict[str, Any],
                              output_file: str):
    """Generate performance comparison report"""
    
    report = {
        'timestamp': time.strftime('%Y-%m-%d %H:%M:%S'),
        'test_configuration': {
            'workspace': os.environ.get('BITBUCKET_WORKSPACE', 'unknown'),
            'repo_slug': os.environ.get('BITBUCKET_REPO_SLUG', 'unknown'),
            'pr_id': os.environ.get('BITBUCKET_PR_ID', 'unknown')
        },
        'full_clone_approach': full_clone_metrics,
        'pr_diff_approach': pr_diff_metrics,
        'comparison': comparison,
        'recommendations': []
    }
    
    # Add recommendations based on results
    if comparison['duration_improvement']['improvement_percent'] > 50:
        report['recommendations'].append(
            f"PR diff approach is {comparison['duration_improvement']['improvement_percent']:.1f}% faster - RECOMMENDED"
        )
    elif comparison['duration_improvement']['improvement_percent'] < 0:
        report['recommendations'].append(
            "PR diff approach is slower - investigate performance issues"
        )
    
    if comparison['memory_improvement']['improvement_percent'] > 50:
        report['recommendations'].append(
            f"PR diff approach uses {comparison['memory_improvement']['improvement_percent']:.1f}% less memory - RECOMMENDED"
        )
    
    if not pr_diff_metrics.get('success', False):
        report['recommendations'].append(
            "PR diff validation failed - check implementation and API access"
        )
    
    # Write report
    with open(output_file, 'w') as f:
        json.dump(report, f, indent=2)
    
    print(f"Performance report saved to: {output_file}")

def main():
    """Main entry point"""
    parser = argparse.ArgumentParser(description='Performance test for validation approaches')
    parser.add_argument('--workspace', required=True, help='Bitbucket workspace')
    parser.add_argument('--repo-slug', required=True, help='Repository slug')
    parser.add_argument('--access-token', required=True, help='Bitbucket access token')
    parser.add_argument('--pr-id', type=int, help='PR ID for diff validation (optional)')
    parser.add_argument('--output', default='performance_report.json', help='Output file')
    
    args = parser.parse_args()
    
    print("Starting performance comparison test...")
    print("=" * 60)
    
    # Run full clone validation
    full_clone_metrics = run_full_clone_validation(
        args.workspace, args.repo_slug, args.access_token
    )
    
    print(f"Full clone validation: {'SUCCESS' if full_clone_metrics.get('success') else 'FAILED'}")
    print(f"Duration: {full_clone_metrics.get('duration_seconds', 0):.2f} seconds")
    print(f"Memory used: {full_clone_metrics.get('memory_used_mb', 0):.2f} MB")
    print()
    
    # Run PR diff validation (if PR ID provided)
    if args.pr_id:
        pr_diff_metrics = run_pr_diff_validation(
            args.workspace, args.repo_slug, args.access_token, args.pr_id
        )
        
        print(f"PR diff validation: {'SUCCESS' if pr_diff_metrics.get('success') else 'FAILED'}")
        print(f"Duration: {pr_diff_metrics.get('duration_seconds', 0):.2f} seconds")
        print(f"Memory used: {pr_diff_metrics.get('memory_used_mb', 0):.2f} MB")
        print()
        
        # Compare performance
        comparison = compare_performance(full_clone_metrics, pr_diff_metrics)
        
        print("PERFORMANCE COMPARISON:")
        print("-" * 30)
        print(f"Duration improvement: {comparison['duration_improvement']['improvement_percent']:.1f}%")
        print(f"Memory improvement: {comparison['memory_improvement']['improvement_percent']:.1f}%")
        print()
        
        # Generate report
        generate_performance_report(full_clone_metrics, pr_diff_metrics, comparison, args.output)
    else:
        print("No PR ID provided - skipping PR diff validation")
        print("To test PR diff validation, provide --pr-id argument")

if __name__ == '__main__':
    main()