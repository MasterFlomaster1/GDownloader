/*
 * Copyright (C) 2024 hstr0100
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.brlns.gdownloader.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.brlns.gdownloader.GDownloader;

/**
 * @author Gabriel / hstr0100 / vertx010
 */
@Slf4j
public class DirectoryUtils{

    /**
     * Shenanigans to deal with heavily fail-prone filesystem operations on Windows
     */
    private static int getHighestDirectoryNumber(String prefix, Path workDir){
        List<String> directoryNames = getDirectoryNames(prefix, workDir);
        if(directoryNames.isEmpty()){
            return 0;
        }

        return directoryNames.stream()
            .mapToInt(DirectoryUtils::extractNumber)
            .max()
            .orElse(0);
    }

    public static Path getNewestDirectoryEntry(String prefix, Path workDir){
        int highest = getHighestDirectoryNumber(prefix, workDir);

        return Paths.get(workDir.toString(), prefix + highest);
    }

    public static void deleteOlderDirectoryEntries(String prefix, Path workDir, int numberToDelete){
        List<String> directoryNames = getDirectoryNames(prefix, workDir);

        if(directoryNames.size() <= numberToDelete){
            log.debug("No older directories to delete {} <= {}", directoryNames.size(), numberToDelete);
            return;
        }

        directoryNames.sort(Comparator.comparingInt(DirectoryUtils::extractNumber));

        int highest = getHighestDirectoryNumber(prefix, workDir);

        for(int i = 0; i < directoryNames.size() - numberToDelete; i++){
            int number = extractNumber(directoryNames.get(i));
            if(number < highest){
                GDownloader.deleteRecursively(
                    Paths.get(workDir.toString(), directoryNames.get(i)));
            }
        }
    }

    public static Path createNewDirectoryEntry(String prefix, Path workDir) throws IOException{
        int highest = getHighestDirectoryNumber(prefix, workDir);

        return Files.createDirectory(Paths.get(workDir.toString(), prefix + (highest + 1)));
    }

    private static List<String> getDirectoryNames(String prefix, Path workDir){
        List<String> directoryNames = new ArrayList<>();

        File dir = workDir.toFile();
        if(dir.isDirectory()){
            for(File file : dir.listFiles()){
                if(file.isDirectory() && file.getName().startsWith(prefix)){
                    directoryNames.add(file.getName());
                }
            }
        }

        return directoryNames;
    }

    private static int extractNumber(String directoryName){
        return Integer.parseInt(directoryName.replaceAll("\\D+", ""));
    }

}
