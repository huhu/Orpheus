/*
 * Copyright (c) 2015 OpenSilk Productions LLC
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

package org.opensilk.music.artwork.shared;

import android.content.Context;

import org.opensilk.common.core.dagger2.ForApplication;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Since were in a library package we cant hardcode the provider authority
 * so we use this to inject it where needed. Corresponds to manifest entry
 * <pre>
 *     android:authorities="${applicationId}.provider.artwork"
 * </pre>
 *
 * Created by drew on 5/1/15.
 */
@Module
public class ArtworkAuthorityModule {
    @Provides @Singleton @Named("artworkauthority")
    public String provideArtworkAuthority(@ForApplication Context context) {
        return context.getPackageName() + ".provider.artwork";
    }
}
