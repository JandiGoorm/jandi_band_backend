#!/usr/bin/env ruby
# This job runs the default branch script after CI. It never checks out a PR.
require 'json'
require 'open3'
require 'yaml'

def api(path, *options)
  output, status = Open3.capture2('gh', 'api', path, *options)
  abort "GitHub API failed for #{path}" unless status.success?
  output.empty? ? nil : JSON.parse(output)
end

event = JSON.parse(File.read(ENV.fetch('GITHUB_EVENT_PATH')))
run = event.fetch('workflow_run')
repo = ENV.fetch('GITHUB_REPOSITORY')
sha = run.fetch('head_sha')
exit unless run['path'] == '.github/workflows/cicd.yml' && run['event'] == 'pull_request' &&
            run['conclusion'] == 'success' && run.dig('head_repository', 'full_name') == repo
links = run.fetch('pull_requests')
exit unless links.size == 1
number = Integer(links.first.fetch('number'))
path = "repos/#{repo}/pulls/#{number}"
pr = api(path)
exit unless pr['state'] == 'open' && !pr['draft'] && pr.dig('user', 'login') == 'dependabot[bot]' &&
            pr.dig('base', 'ref') == 'main' && pr.dig('head', 'repo', 'full_name') == repo &&
            pr.dig('head', 'sha') == sha && pr.dig('head', 'ref').start_with?('dependabot/') &&
            pr.fetch('commits') == 1 && pr.fetch('changed_files').between?(1,100)
commit = api("repos/#{repo}/commits/#{sha}")
exit unless commit.dig('author', 'login') == 'dependabot[bot]' && commit.dig('commit', 'verification', 'verified')
files = api("#{path}/files?per_page=100")
allowed = ENV.fetch('PATCH_FILES').split
exit unless files.length == pr.fetch('changed_files') && files.all? { |f| f['status'] == 'modified' && allowed.include?(f['filename']) }
# Dependabot signs the per-dependency update classification in the commit message.
message = commit.dig('commit','message')
block = message[/^---\s*$\n(updated-dependencies:.*?)(?:^\.\.\.\s*$|^Signed-off-by:|\z)/m, 1]
updates = block && YAML.safe_load(block).fetch('updated-dependencies', [])
patch = updates.is_a?(Array) && !updates.empty? && updates.all? { |d| d['update-type'] == 'version-update:semver-patch' }
# A digest refresh can have no semver classification. Require unchanged tags and lines.
digest_only = files.all? do |f|
  next false unless f['filename'].end_with?('Dockerfile','gateway/compose.yaml')
  lines = f.fetch('patch','').lines
  changed = lines.select { |l| l.start_with?('+','-') }
  next false if changed.empty? || changed.any? { |l| !l.match?(/@sha256:[0-9a-f]{64}\b/) }
  before, after = %w[- +].map do |sign|
    changed.select { |line| line.start_with?(sign) }.map do |line|
      line[1..].chomp.gsub(/@sha256:[0-9a-f]{64}\b/, '@digest')
    end
  end
  !before.empty? && before == after
end
exit unless patch || digest_only
# The SHA guard and the repository's strict required checks protect against a stale CI result.
result = api("#{path}/merge", '-X', 'PUT', '-f', "sha=#{sha}", '-f', 'merge_method=squash')
abort 'Patch was not merged' unless result['merged']
# A merge using GITHUB_TOKEN does not reliably trigger a push workflow.
api("repos/#{repo}/actions/workflows/cicd.yml/dispatches", '-X', 'POST', '-f', 'ref=main')
puts "Merged patch PR ##{number} and requested production CI/CD."
