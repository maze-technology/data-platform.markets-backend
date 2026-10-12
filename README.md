# Data Markets Backend Service

Backend service for the data markets domain, built with Spring Boot and Maze conventions.

## Quality

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=maze-technology_data-markets-backend&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=maze-technology_data-markets-backend)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=maze-technology_data-markets-backend&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=maze-technology_data-markets-backend)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=maze-technology_data-markets-backend&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=maze-technology_data-markets-backend)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=maze-technology_data-markets-backend&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=maze-technology_data-markets-backend)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=maze-technology_data-markets-backend&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=maze-technology_data-markets-backend)
[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=maze-technology_data-markets-backend&metric=sqale_index)](https://sonarcloud.io/summary/new_code?id=maze-technology_data-markets-backend)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=maze-technology_data-markets-backend&metric=coverage)](https://sonarcloud.io/summary/new_code?id=maze-technology_data-markets-backend)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=maze-technology_data-markets-backend&metric=bugs)](https://sonarcloud.io/summary/new_code?id=maze-technology_data-markets-backend)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=maze-technology_data-markets-backend&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=maze-technology_data-markets-backend)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=maze-technology_data-markets-backend&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=maze-technology_data-markets-backend)

## Commands

Check [Makefile](Makefile)

## Configuration

Configuration files are located in the `src/main/resources` directory. Modify `application-local.yml` to suit your needs.

### Configure the GitHub Repository

1. Go to your GitHub repository settings.
2. Navigate to the "Secrets and variables" section and add the following:

   **Secrets:**

   - `DOCKER_PASSWORD` (Your Docker Hub password)

   **Variables:**

   - `DOCKER_USERNAME` (Your Docker Hub username)
   - `DOCKER_REPOSITORY` (The Docker Hub repository where the built image should be pushed by the workflow)
