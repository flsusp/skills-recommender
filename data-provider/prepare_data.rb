#!/usr/bin/env ruby
require 'commander/import'
require 'httparty'

program :version, '0.1'
program :description, 'Downloads Jigsaw data and convert it to CSV'

base_url = 'https://jigsaw.thoughtworks.com/api'

available_skills = {}

command :prepare_for_recommendation do |c|
  c.description = 'Prepare data for recommendation'
  c.syntax = "#{program(:name)} prepare_for_recommendation <output_file> <token>"
  c.option '--working_office STRING', String, 'Working office'

  c.action do |args, options|
    output = open(args.shift || fail('Output file is required'), 'w')
    token = args.shift || fail('The authorization token is required')
    url = "#{base_url}/people"
    result = []
    page = 1
    begin
      query = { 'page' => page }
      query['working_office'] = options.working_office unless options.working_office.nil?
      result = HTTParty.get(url, query: query, headers: { "Authorization" => token })
      result.each do |person|
        skills = HTTParty.get("#{url}/#{person['employeeId']}/skills", headers: { "Authorization" => token })
        skills.each do |skill|
          skill_name = "#{skill['group']['name']}/#{skill['name']}".strip
          skill_id = available_skills.length
          if available_skills.has_key? skill_name
            skill_id = available_skills[skill_name]
          else
            available_skills[skill_name] = skill_id
          end
          line = "#{person['employeeId']},#{skill_id},#{skill['rating']}\n"
          output.write(line)
        end
        sleep 1
      end
      page += 1
    end while result.empty?

    output.close

    available_skills.each_pair do |skill_name, skill_id|
      puts "#{skill_name.gsub(',', ' &')},#{skill_id}"
    end
  end
end
