//
//  LoginView.swift
//  GenericApp
//
//  Created by nuc on 17/06/2020.
//  Copyright © 2020 OpenRemote. All rights reserved.
//

import SwiftUI

struct LoginView: View {
    @State private var realm: String = ""
    
    var body: some View {
        VStack{
            Image("logo").resizable().aspectRatio(contentMode: .fit)
            Text("Connect to your application")
                .font(.title)
            TextField("Project", text: $realm)
                .font(.title)
                .padding()
                .background(Color.gray)
                .cornerRadius(10)
                .padding()
            TextField("Realm", text: $realm)
                .font(.title)
                .padding()
                .background(Color.gray)
                .cornerRadius(10)
                .padding()
            HStack {
                Spacer()
                Button(action: {
                    print("Hey")
                }, label: {
                    Text("Connect")
                })
                    .foregroundColor(Color.white)
                    .padding()
                    .background(Color.green)
                    .cornerRadius(10)
                    .padding()
            }
        }
    }
}

struct LoginView_Previews: PreviewProvider {
    static var previews: some View {
        LoginView()
    }
}
